package io.le.proxy.server.handler;

import io.le.proxy.server.config.ProxyServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Http代理交换机
 */
@Slf4j
public class ProxyExchangeHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public ProxyExchangeHandler(ProxyServerConfig serverConfig, Channel exchangeChannel) {
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
            if(msg instanceof HttpObject) {
                log.debug("Read {} from remote: \r\n{}\r\n{}", msg.getClass().getSimpleName(), msg, exchangeChannel.toString());
            } else {
                log.debug("Read {} from remote: \r\n{}\r\n{}", msg.getClass().getSimpleName(), msg, exchangeChannel.toString());
            }
        }
        exchangeChannel.writeAndFlush(msg);
    }
}
