package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5ProxyServerConnectionHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private final HttpProxyServerConfig serverConfig;
    public Socks5ProxyServerConnectionHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
        System.out.println(msg);
    }
}
