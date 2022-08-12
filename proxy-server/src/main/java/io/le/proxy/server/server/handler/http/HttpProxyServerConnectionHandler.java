package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.config.UsernamePasswordAuth;
import io.le.proxy.server.server.handler.ProxyExchangeHandler;
import io.le.proxy.server.server.ssl.BouncyCastleCertificateGenerator;
import io.le.proxy.server.utils.http.HttpObjectUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 代理服务器ConnectionHandler
 * 用于处理浏览器与代理服务器的连接， 连接成功后从pipeline中移除， 增加HttpProxyServerDispatcherHandler处理浏览器的IO
 */
@Slf4j
public class HttpProxyServerConnectionHandler extends ChannelInboundHandlerAdapter {

    private HttpRequestInfo httpRequestInfo;
    private final ProxyServerConfig serverConfig;
    private ProxyExchangeHandler httpProxyExchangeHandler;
    // private final List<Object> messageQueue = new ArrayList<>();

    public HttpProxyServerConnectionHandler(ProxyServerConfig serverConfig) {
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
        if(msg instanceof HttpRequest) {
            // 建立或获取远端站点的连接， 转发数据
            channelReadHttpRequest(ctx, (HttpRequest) msg);
        } else {
            log.error("Unexpected packet: {}", HttpObjectUtils.stringOf(msg));
        }
    }

    private ChannelFuture channelReadHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
        String proxyAuthorization = request.headers().get("Proxy-Authorization");
        if(serverConfig.getUsernamePasswordAuth() != null) {
            if(proxyAuthorization == null || proxyAuthorization.isEmpty()) {
                response407ProxyAuthenticationRequired(ctx, request, "Please provide Proxy-Authorization")
                        .addListener(ChannelFutureListener.CLOSE);
                return null;
            }

            UsernamePasswordAuth usernamePasswordAuth = serverConfig.getUsernamePasswordAuth();
            String usernamePassword = usernamePasswordAuth.getUsername() + ":" + usernamePasswordAuth.getPassword();

            if(!proxyAuthorization.equals("Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8)))) {
                response407ProxyAuthenticationRequired(ctx, request, "Incorrect proxy username or password")
                        .addListener(ChannelFutureListener.CLOSE);
                return null;
            }
        }

        // 移除Connect
        ctx.pipeline().remove(HttpProxyServerConnectionHandler.class);

        // 连接目标网站并响应200
        if(request.method() == HttpMethod.CONNECT) {
            return connectTargetServer(ctx, request).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    Channel clientChannel = future.channel();
                    // 连接成功， 移除ConnectionHandler, 添加ExchangeHandler
                    log.debug("{} Successfully connected to {}:{}!", clientChannel, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());

                    // 添加Dispatcher
                    httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                    ctx.pipeline().addLast(httpProxyExchangeHandler);

                    response200ProxyEstablished(ctx, request).addListener(future1 -> {
                        if(serverConfig.isCodecMsg()) {
                            // 自签证书
                            X509Certificate x509Certificate = BouncyCastleCertificateGenerator.generateServerCert(httpRequestInfo.getRemoteHost());
                            SslContext sslCtx = SslContextBuilder
                                    .forServer(BouncyCastleCertificateGenerator.serverPriKey, x509Certificate).build();
                            // ctx.pipeline().addFirst(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                            // ctx.pipeline().addFirst(new HttpServerCodec());
                            ctx.pipeline().addFirst(sslCtx.newHandler(ctx.alloc()));
                        } else {
                            ctx.pipeline().remove(HttpServerCodec.class);
                            ctx.pipeline().remove(HttpObjectAggregator.class);
                        }
                    });
                } else {
                    log.error("Failed connect to {}:{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
                    if (ctx.channel().isActive()) {
                        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        ctx.close();
                    }
                }
            });
        }

        // 连接目标网站并发送消息
        return connectTargetServer(ctx, request).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel clientChannel = future.channel();
                // 连接成功， 移除ConnectionHandler, 添加ExchangeHandler
                log.debug("{} Successfully connected to {}:{}!", clientChannel, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());

                // 添加Dispatcher
                httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                ctx.pipeline().addLast(httpProxyExchangeHandler);

                // 转发消息给目标服务器
                if(serverConfig.isCodecMsg()) {
                    // 以下两种写法都行
                    // httpProxyExchangeHandler.channelRead(ctx, request);
                    clientChannel.writeAndFlush(request);
                } else {
                    clientChannel.writeAndFlush(request, clientChannel.newPromise().addListener(future1 -> {
                        ctx.pipeline().remove(HttpServerCodec.class);
                        ctx.pipeline().remove(HttpObjectAggregator.class);
                    }));
                }
            } else {
                log.error("Failed connect to {}:{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
                if (ctx.channel().isActive()) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.close();
                }
            }
        });
    }

    private ChannelFuture connectTargetServer(ChannelHandlerContext ctx, HttpRequest request) {
        httpRequestInfo = new HttpRequestInfo(request);

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

        return bootstrap.connect(httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
    }

    private ChannelFuture response200ProxyEstablished(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(),
                new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection Established"));
        return ctx.writeAndFlush(fullHttpResponse);
    }


    private ChannelFuture response407ProxyAuthenticationRequired(ChannelHandlerContext ctx, HttpRequest request, String reasonPhrase) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(),
                new HttpResponseStatus(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED.code(),
                        reasonPhrase)
        );
        fullHttpResponse.headers().set(HttpHeaderNames.PROXY_AUTHENTICATE, "Basic realm=\"Access to the staging site\"");
        return ctx.writeAndFlush(fullHttpResponse);
    }
}
