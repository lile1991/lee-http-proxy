package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
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
    private final HttpProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public Socks5ProxyExchangeHandler(HttpProxyServerConfig serverConfig, Channel exchangeChannel) {
        this.serverConfig = serverConfig;
        this.exchangeChannel = exchangeChannel;
    }

    /**
     * 从远程服务端读取数据, 转发给客户端
     * @param ctx 连远程服务器
     * @param msg HttpResponse数据
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
