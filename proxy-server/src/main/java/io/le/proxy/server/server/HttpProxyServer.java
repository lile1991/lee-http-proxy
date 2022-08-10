package io.le.proxy.server.server;

import io.le.proxy.server.relay.handler.codec.lee.LeeServerCodec;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.handler.HttpProxyServerConnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Collections;

@Slf4j
public class HttpProxyServer {

    /**
     * 启动代理服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public void start(HttpProxyServerConfig serverConfig) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
        log.debug("HTTP proxy server bind to port: {}, protocol: {}", serverConfig.getPort(), serverConfig.getProxyProtocol());
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws CertificateException, SSLException {
                        if(serverConfig.getProxyProtocol() == HttpProxyServerConfig.ProxyProtocol.HTTPS) {
                            // Support HTTPS proxy protocol
                            SelfSignedCertificate ssc = new SelfSignedCertificate();
                            SslProvider provider =  SslProvider.isAlpnSupported(SslProvider.OPENSSL)  ? SslProvider.OPENSSL : SslProvider.JDK;
                            SslContext sslCtx = SslContextBuilder
                                    .forServer(ssc.certificate(), ssc.privateKey())
                                    .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .sslProvider(provider)
                                    //支持的cipher
                                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                                            ApplicationProtocolConfig.Protocol.ALPN,
                                            // 目前 OpenSsl 和 JDK providers只支持NO_ADVERTISE
                                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                            // 目前 OpenSsl 和 JDK providers只支持ACCEPT
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                            ApplicationProtocolNames.HTTP_2,
                                            ApplicationProtocolNames.HTTP_1_1))
                                    .build();
                            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                        }

                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                            .addLast(new HttpProxyServerConnectionHandler(serverConfig));

                        switch (serverConfig.getProxyProtocol()) {
                            case LEE: ch.pipeline().addLast(new LeeServerCodec()); break;
                        }
                    }
                }).bind(serverConfig.getPort());
    }
}
