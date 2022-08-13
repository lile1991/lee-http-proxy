package io.le.proxy.server.handler.http.relay;

import io.le.proxy.server.config.ProxyProtocolEnum;
import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.ProxyExchangeHandler;
import io.le.proxy.server.handler.http.HttpRequestInfo;
import io.le.proxy.server.handler.https.SslHandlerCreator;
import io.le.proxy.server.handler.socks5.Socks5InitialRequestHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class RelayToSocks5ProxyInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public RelayToSocks5ProxyInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException, CertificateException {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        // Socks5MessageByteBuf
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialResponseDecoder());
        ch.pipeline().addLast(Socks5ClientEncoder.DEFAULT);
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialRequestHandler(serverConfig));
        log.debug("Add HttpClientCodec to pipeline");

        if(httpRequestInfo.isSsl()) {
            // HTTPS连接需要处理一次Connect响应， 需要在ProxyExchangeHandler读取到代理服务器响应后
            ch.pipeline().addLast(RelayShakeHandsHandler.class.getSimpleName(), new RelayShakeHandsHandler(serverConfig, proxyServerChannel));
        } else {
            ch.pipeline().addLast(ProxyExchangeHandler.class.getSimpleName(), new ProxyExchangeHandler(serverConfig, proxyServerChannel));
        }
        log.debug("Add ProxyExchangeHandler to pipeline");
    }
}
