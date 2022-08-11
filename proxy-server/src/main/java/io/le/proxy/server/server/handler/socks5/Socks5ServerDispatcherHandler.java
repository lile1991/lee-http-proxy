package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.utils.http.HttpObjectUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5ServerDispatcherHandler extends ChannelInboundHandlerAdapter {
    private final HttpProxyServerConfig serverConfig;
    private final Channel proxyClientChannel;

    public Socks5ServerDispatcherHandler(HttpProxyServerConfig serverConfig, Channel proxyClientChannel) {
        this.serverConfig = serverConfig;
        this.proxyClientChannel = proxyClientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(log.isDebugEnabled()) {
            if(serverConfig.isCodecSsl()) {
                log.debug("{} send to remote: \r\n{}", proxyClientChannel.toString(), HttpObjectUtils.stringOf(msg));
            } else {
                log.debug("{} send to remote: \r\n{}", proxyClientChannel.toString(), msg);
            }
        }
        proxyClientChannel.writeAndFlush(msg);
    }
}
