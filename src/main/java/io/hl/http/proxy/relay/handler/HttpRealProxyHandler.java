package io.hl.http.proxy.relay.handler;

import io.hl.http.proxy.relay.config.HttpProxyRelayServerConfig;
import io.hl.http.proxy.server.handler.HttpRequestInfo;
import io.hl.http.proxy.utils.http.HttpObjectUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

/**
 * 与代理服务器Handler
 */
@Slf4j
public class HttpRealProxyHandler extends ChannelInboundHandlerAdapter {
    private final Channel relayChannel;
    private final HttpProxyRelayServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;
    public static final AttributeKey<Object> RESPONSE_ATTRIBUTE_KEY = AttributeKey.newInstance("response");

    public HttpRealProxyHandler(Channel relayChannel, HttpProxyRelayServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.relayChannel = relayChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    /**
     * 从代理服务器读取数据, 将数据转发到Browser客户端
     * @param ctx Relay Server <-> Proxy Server
     * @param msg 读取到的响应 HttpResponse
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        proxyServerResponse(ctx, msg);
    }

    private void proxyServerResponse(ChannelHandlerContext ctx, Object msg) {
        /*if(msg instanceof ByteBuf) {
            if (serverConfig.isCodecSsl()) {
                // Need to parse HTTPS packets, add SSL processor
                // toProxyChannel增加SSLHandler
                SslContext clientSslCtx = SslContextBuilder
                        .forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                ctx.channel().pipeline().addFirst(clientSslCtx.newHandler(ctx.alloc(), httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort()));

                log.debug("{} Add SSLHandler to ToProxyChannel, pipeline: {}", ctx.channel(), ctx.pipeline().names());
                ctx.fireChannelRead(msg);
                return;
            }
            if(ctx.pipeline().get(SslHandler.class) != null) {
                ctx.fireChannelRead(msg);
            }
        }*/

        if(log.isDebugEnabled()) {
            if(serverConfig.isCodecSsl()) {
                log.debug("{} Response {} from proxy: \r\n{}", ctx.channel().toString(), msg.getClass().getSimpleName(), HttpObjectUtils.stringOf(msg));
            } else {
                log.debug("{} Response {} from proxy: \r\n{}", ctx.channel().toString(), msg.getClass().getSimpleName(), msg);
            }
        }

        Object request = relayChannel.attr(HttpRelayDispatcherHandler.REQUEST_ATTRIBUTE_KEY).get();
        relayChannel.attr(RESPONSE_ATTRIBUTE_KEY).set(msg);
        relayChannel.writeAndFlush(msg).addListeners(future -> {
            if(request instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) request;
                if(httpRequest.method() == HttpMethod.CONNECT) {
                    relayChannel.pipeline().remove(HttpServerCodec.class);
                    relayChannel.pipeline().remove(HttpObjectAggregator.class);
                    log.info("Remove HttpServerCodec from relayChannel: {}", relayChannel.pipeline().names());

                    if(!serverConfig.isCodecSsl()) {
                        // 不解码HTTPS， HttpClientCodec 也就无效了, 可以移除掉
                        ctx.channel().pipeline().remove(HttpObjectAggregator.class);
                        ctx.channel().pipeline().remove(HttpClientCodec.class);
                        log.info("{} Remove HttpClientCodec from ToProxyChannel, pipeline: {}", ctx.channel(), ctx.pipeline().names());
                    }
                }
            }
        });
    }
}