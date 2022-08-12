package io.le.proxy.server.server.handler.http;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.handler.ProxyExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * 与目标网站的Handler
 */
public class HttpConnectToRemoteInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpConnectToRemoteInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
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
        ch.pipeline().addLast(HttpConnectToRemoteInitHandler.class.getSimpleName(), new ProxyExchangeHandler(serverConfig, proxyServerChannel));
    }
}
