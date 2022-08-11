package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.utils.http.HttpObjectUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpProxyClientHandler extends ChannelInboundHandlerAdapter {
    private final HttpProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;

    public HttpProxyClientHandler(HttpProxyServerConfig serverConfig, Channel proxyServerChannel) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
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
                log.debug("{} read {} from remote: \r\n{}", proxyServerChannel.toString(), msg.getClass().getSimpleName(), HttpObjectUtils.stringOf(msg));
            } else {
                log.debug("{} read {} from remote: \r\n{}", proxyServerChannel.toString(), msg.getClass().getSimpleName(), msg);
            }
        }
        proxyServerChannel.writeAndFlush(msg);
    }
}
