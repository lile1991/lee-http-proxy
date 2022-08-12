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
 * 中继HTTP代理
 */
@Slf4j
public class HttpConnectToRemoteReplayHandler extends HttpConnectToRemoteHandler {

    public HttpConnectToRemoteReplayHandler(ProxyServerConfig serverConfig) {
        super(serverConfig);
    }

    /**
     * 连接到远端代理机器
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpRequest request = (HttpRequest) msg;
        super.channelRead(ctx, msg);
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
                .handler(new HttpConnectToRemoteInitHandler(ctx.channel(), serverConfig, httpRequestInfo))
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
