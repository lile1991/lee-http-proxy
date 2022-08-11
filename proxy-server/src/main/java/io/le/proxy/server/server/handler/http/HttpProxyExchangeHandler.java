package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.utils.http.HttpObjectUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Http代理交换机
 */
@Slf4j
public class HttpProxyExchangeHandler extends ChannelInboundHandlerAdapter {
    private final HttpProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public HttpProxyExchangeHandler(HttpProxyServerConfig serverConfig, Channel exchangeChannel) {
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
