package io.le.proxy.server.handler.http.relay.http;

import io.le.proxy.server.config.ProxyProtocolEnum;
import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.ExchangeHandler;
import io.le.proxy.server.handler.http.HttpRequestInfo;
import io.le.proxy.server.handler.https.SslHandlerCreator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class HttpRelayInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpRelayInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException, CertificateException {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        if(serverConfig.getRelayServerConfig().getRelayProtocol() == ProxyProtocolEnum.HTTPS) {
            ch.pipeline().addLast(SslHandlerCreator.forClient(ch.alloc()));
        }
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
        log.debug("Add HttpClientCodec to pipeline");

        if(httpRequestInfo.isSsl()) {
            // HTTPS连接需要处理一次Connect响应， 需要在ProxyExchangeHandler读取到代理服务器响应后
            ch.pipeline().addLast(HttpsRelayShakeHandsHandler.class.getSimpleName(), new HttpsRelayShakeHandsHandler(serverConfig, proxyServerChannel));
        } else {
            ch.pipeline().addLast(ExchangeHandler.class.getSimpleName(), new ExchangeHandler(serverConfig, proxyServerChannel));
        }
        log.debug("Add ProxyExchangeHandler to pipeline");
    }
}
