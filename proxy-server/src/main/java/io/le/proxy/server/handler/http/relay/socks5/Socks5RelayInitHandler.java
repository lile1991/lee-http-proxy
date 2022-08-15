package io.le.proxy.server.handler.http.relay.socks5;

import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class Socks5RelayInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public Socks5RelayInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException, CertificateException {
        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        // Socks5MessageByteBuf
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialResponseDecoder());
        ch.pipeline().addLast(Socks5ClientEncoder.DEFAULT);
        // sock5 init
        ch.pipeline().addLast(new Socks5RelayInitialResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
        log.debug("Add Socks5ClientEncoder to pipeline");

        // ch.pipeline().addLast(ProxyExchangeHandler.class.getSimpleName(), new ProxyExchangeHandler(serverConfig, proxyServerChannel));
        // log.debug("Add ProxyExchangeHandler to pipeline");
    }
}
