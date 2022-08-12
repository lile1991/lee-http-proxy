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
 * 接收代理请求
 */
@Slf4j
public class HttpAcceptConnectHandler extends ChannelInboundHandlerAdapter {

    private final ProxyServerConfig serverConfig;
    // private final List<Object> messageQueue = new ArrayList<>();

    public HttpAcceptConnectHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 收到客户端(比如浏览器)的连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("CONN {}", ctx);
    }

    /**
     * 处理代理连接请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("{} read: {}\r\n{}", ctx.name(), msg, ctx.channel());
        HttpRequest request = (HttpRequest) msg;
        String proxyAuthorization = request.headers().get(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
        if(serverConfig.getUsernamePasswordAuth() != null) {
            if(proxyAuthorization == null || proxyAuthorization.isEmpty()) {
                log.debug("Please provide Proxy-Authorization\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx, request, "Please provide Proxy-Authorization")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            UsernamePasswordAuth usernamePasswordAuth = serverConfig.getUsernamePasswordAuth();
            String usernamePassword = usernamePasswordAuth.getUsername() + ":" + usernamePasswordAuth.getPassword();

            if(!proxyAuthorization.equals("Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8)))) {
                log.debug("Incorrect proxy username or password\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx, request, "Incorrect proxy username or password")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }
        }

        // 移除自己
        ctx.pipeline().remove(ctx.name());
        ctx.fireChannelRead(msg);
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
