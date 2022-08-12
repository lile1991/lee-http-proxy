package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.handler.ProxyExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * 与目标网站的Handler
 */
public class HttpConnectToHostInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpConnectToHostInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));

        // 解码器放到HttpConnectToRemoteHandler去添加
        /*if(httpRequestInfo.isSsl()) {
            if(serverConfig.isCodecMsg()) {
                SslContext sslCtx = SslContextBuilder
                        .forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                ch.pipeline().addFirst(sslCtx.newHandler(ch.alloc(), httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort()));
                // ch.pipeline().addLast(new HttpContentDecompressor());
                ch.pipeline().addLast(new HttpClientCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
            }
        } else {
            // ch.pipeline().addLast(new HttpContentDecompressor());
            ch.pipeline().addLast(new HttpClientCodec());
            ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
        }*/
        ch.pipeline().addLast(HttpConnectToHostInitHandler.class.getSimpleName(), new ProxyExchangeHandler(serverConfig, proxyServerChannel));
    }
}
