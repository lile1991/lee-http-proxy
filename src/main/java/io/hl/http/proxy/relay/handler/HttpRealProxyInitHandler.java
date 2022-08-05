package io.hl.http.proxy.relay.handler;

import io.hl.http.proxy.relay.config.HttpProxyRelayServerConfig;
import io.hl.http.proxy.relay.handler.codec.lee.LeeClientCodec;
import io.hl.http.proxy.server.handler.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 * 与代理服务器InitHandler
 */
public class HttpRealProxyInitHandler extends ChannelInitializer<Channel> {
    private final Channel relayChannel;
    private final HttpProxyRelayServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpRealProxyInitHandler(Channel relayChannel, HttpProxyRelayServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.relayChannel = relayChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) {
        // ch.pipeline().addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
        switch (serverConfig.getRelayProtocol()) {
            case LEE: ch.pipeline().addLast(new LeeClientCodec()); break;
        }
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
        ch.pipeline().addLast(new HttpRealProxyHandler(relayChannel, serverConfig, httpRequestInfo));
    }
}
