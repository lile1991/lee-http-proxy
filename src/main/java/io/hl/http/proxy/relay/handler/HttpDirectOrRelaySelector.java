package io.hl.http.proxy.relay.handler;

import io.hl.http.proxy.relay.config.HttpProxyRelayServerConfig;
import io.hl.http.proxy.server.handler.HttpProxyServerConnectionHandler;
import io.hl.http.proxy.server.handler.HttpRequestInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpDirectOrRelaySelector extends ChannelInboundHandlerAdapter {
    final HttpProxyRelayServerConfig serverConfig;
    public HttpDirectOrRelaySelector(HttpProxyRelayServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 从客户端(比如浏览器)读数据
     * @param ctx 与客户端的连接
     * @param msg 消息 HttpConnect、HttpRequest、HttpContent、SSL请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息: {}", msg);
        if(msg instanceof HttpRequest) {
            selector(ctx, (HttpRequest) msg);
        } else {
            log.error("收到莫名消息: {}", msg);
        }

    }

    private void selector(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        HttpRequestInfo requestInfo = new HttpRequestInfo(request);
        String remoteHost = requestInfo.getRemoteHost();
        ctx.pipeline().remove(HttpDirectOrRelaySelector.class);
        if(remoteHost.contains("ipinfo")) {
            log.info("Host address {} go direct!", remoteHost);
            HttpProxyServerConnectionHandler httpProxyServerConnectionHandler = new HttpProxyServerConnectionHandler(serverConfig);
            ctx.pipeline().addLast(httpProxyServerConnectionHandler);
            httpProxyServerConnectionHandler.channelRead(ctx, request);
        } else {
            HttpRelayConnectionHandler httpRelayConnectionHandler = new HttpRelayConnectionHandler(serverConfig);
            ctx.pipeline().addLast(httpRelayConnectionHandler);
            httpRelayConnectionHandler.channelRead(ctx, request);
        }
    }
}
