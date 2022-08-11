package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.utils.http.HttpObjectUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Socks5代理交换机
 */
@Slf4j
public class Socks5ProxyExchangeHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public Socks5ProxyExchangeHandler(ProxyServerConfig serverConfig, Channel exchangeChannel) {
        this.serverConfig = serverConfig;
        this.exchangeChannel = exchangeChannel;
    }

    /**
     * 将读取到的数据转发
     * @param ctx channel
     * @param msg 数据包
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(log.isDebugEnabled()) {
            if(serverConfig.isCodecSsl()) {
                log.debug("{} read {} from remote: \r\n{}", exchangeChannel.toString(), msg.getClass().getSimpleName(), HttpObjectUtils.stringOf(msg));
            } else {
                log.debug("{} read {} from remote: \r\n{}", exchangeChannel.toString(), msg.getClass().getSimpleName(), msg);
            }
        }
        exchangeChannel.writeAndFlush(msg);
    }
}
