package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.handler.ProxyExchangeHandler;
import io.le.proxy.server.server.ssl.BouncyCastleCertificateGenerator;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;

/**
 * HTTP代理
 */
@Slf4j
public class HttpConnectToHostHandler extends ChannelInboundHandlerAdapter {

    HttpRequestInfo httpRequestInfo;
    final ProxyServerConfig serverConfig;
    ProxyExchangeHandler httpProxyExchangeHandler;
    // private final List<Object> messageQueue = new ArrayList<>();

    public HttpConnectToHostHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 连接到远端服务器
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("{} read: {}\r\n{}", ctx.name(), msg, ctx.channel());
        HttpRequest request = (HttpRequest) msg;

        ctx.pipeline().remove(ctx.name());

        // 连接目标网站并响应200
        if(request.method() == HttpMethod.CONNECT) {
            connectTargetServer(ctx, request).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    Channel clientChannel = future.channel();
                    // 连接成功， 移除ConnectionHandler, 添加ExchangeHandler
                    log.debug("Successfully connected to {}:{}!\r\n{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort(), clientChannel);

                    httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                    ctx.pipeline().addLast(httpProxyExchangeHandler);

                    log.debug("Connection Established\r\n{}", ctx);
                    response200ProxyEstablished(ctx, request).addListener(future1 -> {
                        if(serverConfig.isCodecMsg()) {
                            // 解码与客户端的HTTPS消息
                            X509Certificate x509Certificate = BouncyCastleCertificateGenerator.generateServerCert(httpRequestInfo.getRemoteHost());
                            SslContext sslCtxForServer = SslContextBuilder
                                    .forServer(BouncyCastleCertificateGenerator.serverPriKey, x509Certificate).build();
                            // ctx.pipeline().addFirst(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                            // ctx.pipeline().addFirst(new HttpServerCodec());
                            ctx.pipeline().addFirst(sslCtxForServer.newHandler(ctx.alloc()));

                            // 解码与目标服务器的HTTP(s)消息
                            SslContext sslCtxForClient = SslContextBuilder
                                    .forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                            clientChannel.pipeline().addFirst(sslCtxForClient.newHandler(clientChannel.alloc(), httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort()));
                            clientChannel.pipeline().addBefore(HttpConnectToHostInitHandler.class.getSimpleName(), null, new HttpClientCodec());
                            clientChannel.pipeline().addBefore(HttpConnectToHostInitHandler.class.getSimpleName(), null, new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                            log.info("Add HttpClientCodec to pipeline");
                        } else {
                            // 不解码消息， 移除代理服务器的解码器
                            ctx.pipeline().remove(HttpServerCodec.class);
                            ctx.pipeline().remove(HttpObjectAggregator.class);
                            log.info("Remove HttpServerCodec from pipeline");
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

            return;
        }

        // 连接目标网站并发送消息
        connectTargetServer(ctx, request).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel clientChannel = future.channel();
                // 连接成功， 移除ConnectionHandler, 添加ExchangeHandler
                log.debug("Successfully connected to {}:{}!\r\n{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort(), clientChannel);

                // 添加Dispatcher
                httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                ctx.pipeline().addLast(httpProxyExchangeHandler);

                // 转发消息给目标服务器

                log.info("Add HttpClientCodec to pipeline");
                clientChannel.pipeline().addBefore(HttpConnectToHostInitHandler.class.getSimpleName(), null, new HttpClientCodec());
                clientChannel.pipeline().addBefore(HttpConnectToHostInitHandler.class.getSimpleName(), null, new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));

                log.info("WriteAndFlush msg: {}", request.method() + " " + request.uri());
                if(serverConfig.isCodecMsg()) {
                    // 以下两种写法都行
                    // httpProxyExchangeHandler.channelRead(ctx, request);
                    clientChannel.writeAndFlush(request);
                } else {
                    clientChannel.writeAndFlush(request, clientChannel.newPromise().addListener(future1 -> {
                        ctx.pipeline().remove(HttpServerCodec.class);
                        ctx.pipeline().remove(HttpObjectAggregator.class);
                        log.info("Remove HttpServerCodec from pipeline");

                        clientChannel.pipeline().remove(HttpClientCodec.class);
                        clientChannel.pipeline().remove(HttpObjectAggregator.class);
                        log.info("Remove HttpClientCodec from pipeline");
                    }));
                }
            } else {
                log.error("Connected failed {}:{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
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
                .handler(new HttpConnectToHostInitHandler(ctx.channel(), serverConfig, httpRequestInfo))
                ;

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
}
