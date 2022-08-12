package io.le.proxy.server.server.handler.http.relay;

import io.le.proxy.server.server.config.ProxyServerConfig;
import io.le.proxy.server.server.handler.ProxyExchangeHandler;
import io.le.proxy.server.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class HttpConnectToProxyInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpConnectToProxyInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
        log.debug("Add HttpClientCodec to pipeline");

        if(httpRequestInfo.isSsl()) {
            // HTTPS连接需要处理一次Connect响应， 需要在ProxyExchangeHandler读取到代理服务器响应后
            ch.pipeline().addLast(HttpsConnectedToShakeHandsHandler.class.getSimpleName(), new HttpsConnectedToShakeHandsHandler(serverConfig, proxyServerChannel));
        } else {
            ch.pipeline().addLast(ProxyExchangeHandler.class.getSimpleName(), new ProxyExchangeHandler(serverConfig, proxyServerChannel));
        }
        log.debug("Add ProxyExchangeHandler to pipeline");
    }
}
