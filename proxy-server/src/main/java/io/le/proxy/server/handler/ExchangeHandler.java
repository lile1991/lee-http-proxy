package io.le.proxy.server.handler;

import io.le.proxy.server.config.ProxyServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 交换机
 */
@Slf4j
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public ExchangeHandler(ProxyServerConfig serverConfig, Channel exchangeChannel) {
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
                log.debug("Read a message {} from remote: \r\n{}\r\n{}", msg.getClass().getSimpleName(), msg, exchangeChannel);
            } else {
                log.debug("Read a message {} from remote: \r\n{}\r\n{}", msg.getClass().getSimpleName(), msg, exchangeChannel);
            }
        }

        log.debug("Forward the message. \r\n{}\r\n{}", msg, exchangeChannel);
        exchangeChannel.writeAndFlush(msg);
    }
}