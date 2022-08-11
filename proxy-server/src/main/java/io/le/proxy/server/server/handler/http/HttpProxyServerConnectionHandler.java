package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.ssl.BouncyCastleCertificateGenerator;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理服务器ConnectionHandler
 * 用于处理浏览器与代理服务器的连接， 连接成功后从pipeline中移除， 增加HttpProxyServerDispatcherHandler处理浏览器的IO
 */
@Slf4j
public class HttpProxyServerConnectionHandler extends ChannelInboundHandlerAdapter {

    private HttpRequestInfo httpRequestInfo;
    private final HttpProxyServerConfig serverConfig;
    private HttpProxyServerDispatcherHandler httpProxyServerDispatcherHandler;
    private ChannelFuture clientChannelFuture;
    private final List<Object> messageQueue = new ArrayList<>();

    public HttpProxyServerConnectionHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 收到客户端(比如浏览器)的连接
     * @param ctx 与客户端的连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("{} CONN ", ctx);
    }

    /**
     * 从客户端(比如浏览器)读数据
     * @param ctx 与客户端的连接
     * @param msg 消息 HttpConnect、HttpRequest、HttpContent、SSL请求
     * @throws Exception 读取数据异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("{}, channelRead: {}", ctx.channel(), msg);
        if(clientChannelFuture == null) {
            if(msg instanceof HttpRequest) {
                // 建立或获取远端站点的连接， 转发数据
                clientChannelFuture = channelReadHttpRequest(ctx, (HttpRequest) msg);
            } else {
                log.error("收到莫名为消息: {}", msg);
            }
        } else {
            /*if(msg instanceof LastHttpContent) {
                return;
            }*/
            // HTTPS请求 && 服务端配置是需要解码
            if(msg instanceof ByteBuf) {
                if(serverConfig.isCodecSsl()) {
                    // 自签证书
                    X509Certificate x509Certificate = BouncyCastleCertificateGenerator.generateServerCert(httpRequestInfo.getRemoteHost());
                    SslContext sslCtx = SslContextBuilder
                            .forServer(BouncyCastleCertificateGenerator.serverPriKey, x509Certificate).build();
                    ctx.pipeline().addFirst(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                    ctx.pipeline().addFirst(new HttpServerCodec());
                    ctx.pipeline().addFirst(sslCtx.newHandler(ctx.alloc()));

                    log.debug("Add ssl handler to proxy server, ctx.pipeline: {}", ctx.pipeline().names());
                    // 重走一遍 channelRead ， 解析HTTPS请求包
                    ctx.pipeline().fireChannelRead(msg);
                    return;
                }
            }

            synchronized (messageQueue) {
                if (httpProxyServerDispatcherHandler == null) {
                    // 远端连接尚未建立， 消息暂存到队列中
                    // 在连接成功后（clientChannelFuture.addListener 被调用）， messageQueue会被一次性全部转发并清空
                    messageQueue.add(msg);
                } else {
                    // 远程连接已经建立， 直接交给DispatcherHandler转发
                    httpProxyServerDispatcherHandler.channelRead(ctx, msg);
                }
            }
        }
    }

    private ChannelFuture channelReadHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
        String proxyAuthorization = request.headers().get("Proxy-Authorization");
        /*if(proxyAuthorization == null || proxyAuthorization.isEmpty()) {
            response407ProxyAuthenticationRequired(ctx, request);
            return null;
        }*/

        if(request.method() == HttpMethod.CONNECT) {
            response200ProxyEstablished(ctx, request);
        } else {
            messageQueue.add(request);
        }

        // 连接目标网站
        return createProxyClientChannel(ctx, request);
    }

    private ChannelFuture createProxyClientChannel(ChannelHandlerContext ctx, HttpRequest request) {
        httpRequestInfo = new HttpRequestInfo(request);
        if(httpRequestInfo.isSsl()) {
            ctx.pipeline().remove(HttpServerCodec.class);
            ctx.pipeline().remove(HttpObjectAggregator.class);
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 45 * 1000)
                // Bind remote ip and port
                // .localAddress(serverIp, randomSystemPort)
                // Bind local ip and port
                // .remoteAddress(serverIp, randomSystemPort)
                .handler(new HttpProxyClientInitHandler(ctx.channel(), serverConfig, httpRequestInfo));

        if(serverConfig.getLocalAddress() != null) {
            // Bind local net address
            bootstrap.remoteAddress(serverConfig.getLocalAddress());
        }

        ChannelFuture clientChannelFuture = bootstrap.connect(httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
        clientChannelFuture.addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel clientChannel = future.channel();
                // 连接成功， 移除Connector, 添加Dispatcher
                log.debug("{} Successfully connected to {}:{}!", clientChannel, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
                // 将客户端连接放到channel的AttributeMap中， 所有Handler都可以读到
                synchronized (messageQueue) {
                    if(ctx.pipeline().get(HttpProxyServerConnectionHandler.class) != null) {
                        // 移除Connect
                        ctx.pipeline().remove(HttpProxyServerConnectionHandler.class);
                        // 添加Dispatcher
                        httpProxyServerDispatcherHandler = new HttpProxyServerDispatcherHandler(serverConfig, httpRequestInfo, clientChannel);
                        ctx.pipeline().addLast(httpProxyServerDispatcherHandler);
                    }

                    // 消费掉消息队列
                    for (Object msg : messageQueue) {
                        httpProxyServerDispatcherHandler.channelRead(ctx, msg);
                    }
                    messageQueue.clear();
                }
            } else {
                log.error("Failed connect to {}:{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
                messageQueue.clear();
                if (ctx.channel().isActive()) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                ctx.close();
            }
        });
        return clientChannelFuture;
    }

    private void response200ProxyEstablished(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(),
                new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection Established"));
        ctx.writeAndFlush(fullHttpResponse);
    }


    private void response407ProxyAuthenticationRequired(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(),
                new HttpResponseStatus(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED.code(),
                        "Please provide Proxy-Authorization, sample: RELAY:host:port:username:password or DIRECT:username:password")
        );
        fullHttpResponse.headers().set(HttpHeaderNames.PROXY_AUTHENTICATE, "Basic realm=\"Access to the staging site\"");
        ctx.writeAndFlush(fullHttpResponse);
    }
}
