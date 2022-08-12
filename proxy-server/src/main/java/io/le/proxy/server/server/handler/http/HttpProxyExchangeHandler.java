package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Http代理交换机
 */
@Slf4j
public class HttpProxyExchangeHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public HttpProxyExchangeHandler(ProxyServerConfig serverConfig, Channel exchangeChannel) {
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
                log.debug("{} read {} from remote: \r\n{}", exchangeChannel.toString(), msg.getClass().getSimpleName(), ((HttpObject) msg).decoderResult());
            } else {
                log.debug("{} read {} from remote: \r\n{}", exchangeChannel.toString(), msg.getClass().getSimpleName(), msg);
            }
        }
        exchangeChannel.writeAndFlush(msg);
    }
}
