package io.le.http.proxy.server.handler;

import io.le.http.proxy.server.config.HttpProxyServerConfig;
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
public class HttpProxyClientInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final HttpProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpProxyClientInitHandler(Channel proxyServerChannel, HttpProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        if(httpRequestInfo.isSsl()) {
            if(serverConfig.isCodecSsl()) {
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
        }
        ch.pipeline().addLast(new HttpProxyClientHandler(serverConfig, proxyServerChannel));
    }
}
