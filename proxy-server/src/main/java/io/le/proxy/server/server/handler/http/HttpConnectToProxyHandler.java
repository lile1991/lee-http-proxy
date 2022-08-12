package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.NetAddress;
import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.config.RelayServerConfig;
import io.le.proxy.server.server.config.UsernamePasswordAuth;
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

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 中继， 连接到另一个HTTP代理
 */
@Slf4j
public class HttpConnectToProxyHandler extends ChannelInboundHandlerAdapter {

    HttpRequestInfo httpRequestInfo;
    final ProxyServerConfig serverConfig;
    ProxyExchangeHandler httpProxyExchangeHandler;

    public HttpConnectToProxyHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 连接到远端代理机器
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("{} read: {}\r\n{}", ctx.name(), msg, ctx.channel());
        HttpRequest request = (HttpRequest) msg;

        // 设置远程代理服务器密码
        UsernamePasswordAuth relayUsernamePasswordAuth = serverConfig.getRelayServerConfig().getRelayUsernamePasswordAuth();
        if(relayUsernamePasswordAuth == null) {
            request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
        } else {
            request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION.toString(),
                    "Basic " + Base64.getEncoder().encodeToString(
                            (relayUsernamePasswordAuth.getUsername() + ":" + relayUsernamePasswordAuth.getPassword()).getBytes(StandardCharsets.UTF_8)
                    )
            );
        }

        ctx.pipeline().remove(ctx.name());

        // 连接目标代理并响应200
        if(request.method() == HttpMethod.CONNECT) {
            connectTargetProxy(ctx, request).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    Channel clientChannel = future.channel();
                    // 连接成功
                    log.debug("Successfully connected to {}:{}!\r\n{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort(), clientChannel);

                    // 添加ExchangeHandler
                    httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                    ctx.pipeline().addLast(httpProxyExchangeHandler);

                    // 发送连接请求
                    clientChannel.writeAndFlush(request);

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
        connectTargetProxy(ctx, request).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel clientChannel = future.channel();
                // 连接成功， 移除ConnectionHandler, 添加ExchangeHandler
                log.debug("Successfully connected to {}:{}!\r\n{}", httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort(), clientChannel);

                // 添加Dispatcher
                httpProxyExchangeHandler = new ProxyExchangeHandler(serverConfig, clientChannel);
                ctx.pipeline().addLast(httpProxyExchangeHandler);

                // 转发消息给目标服务器

                log.info("WriteAndFlush msg: {}", request.method() + " " + request.uri());
                // 以下两种写法都行
                // httpProxyExchangeHandler.channelRead(ctx, request);
                clientChannel.writeAndFlush(request);
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

    private ChannelFuture connectTargetProxy(ChannelHandlerContext ctx, HttpRequest request) {
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
                .handler(new HttpConnectToProxyInitHandler(ctx.channel(), serverConfig, httpRequestInfo))
                ;

        if(serverConfig.getLocalAddress() != null) {
            // Bind local net address
            bootstrap.remoteAddress(serverConfig.getLocalAddress());
        }

        RelayServerConfig relayServerConfig = serverConfig.getRelayServerConfig();
        NetAddress relayNetAddress = relayServerConfig.getRelayNetAddress();
        return bootstrap.connect(relayNetAddress.getRemoteHost(), relayNetAddress.getRemotePort());
    }

    private ChannelFuture response200ProxyEstablished(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(),
                new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection Established"));
        return ctx.writeAndFlush(fullHttpResponse);
    }
}
