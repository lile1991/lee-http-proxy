package io.hl.http.proxy.server.handler;

import io.hl.http.proxy.server.config.HttpProxyServerConfig;
import io.hl.http.proxy.utils.http.HttpObjectUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 连接建立后， 由该Handler处理与浏览器的IO， HttpProxyServerConnectorHandler会从pipeline中移除
 */
@Slf4j
public class HttpProxyServerDispatcherHandler extends ChannelInboundHandlerAdapter {
    private final HttpProxyServerConfig serverConfig;
    @Getter
    private final HttpRequestInfo httpRequestInfo;
    private final Channel proxyClientChannel;

    public HttpProxyServerDispatcherHandler(HttpProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo, Channel proxyClientChannel) {
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
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
