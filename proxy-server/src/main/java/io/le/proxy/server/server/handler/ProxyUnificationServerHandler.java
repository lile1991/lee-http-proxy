package io.le.proxy.server.server.handler;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.config.ProxyProtocolEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * 用于同时支持 HTTP、HTTPS、SOCKS4、SOCKS5 代理协议
 */
@Slf4j
public class ProxyUnificationServerHandler extends ChannelInboundHandlerAdapter {

    private final HttpProxyServerConfig serverConfig;

    public ProxyUnificationServerHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProxyProtocolEnum protocol = parseProxyProtocol((ByteBuf) msg);
        if(protocol == null) {
            return;
        }

        if(!serverConfig.getProxyProtocols().contains(protocol)) {
            log.warn("The proxy server does not support the {} protocol!", protocol);
            ctx.close();
            return;
        }

        ChannelPipeline p = ctx.pipeline();
        switch (protocol) {
            case HTTP: addHttpSupport(ctx); break;
            case HTTPS: addHttpsSupport(ctx); break;
            case SOCKS4a:
                p.addAfter(ctx.name(), null, Socks4ServerEncoder.INSTANCE);
                p.addAfter(ctx.name(), null, new Socks4ServerDecoder());
                break;
            case SOCKS5:
                p.addAfter(ctx.name(), null, Socks5ServerEncoder.DEFAULT);
                p.addAfter(ctx.name(), null, new Socks5InitialRequestDecoder());
                break;
        }

        /*if (serverConfig.getProxyProtocols().contains(ProxyProtocolEnum.LEE)) {
            p.addBefore(ctx.name(), null, new LeeServerCodec());
        }*/

        ctx.pipeline().remove(getClass());
        logKnownVersion(ctx, protocol);
        super.channelRead(ctx, msg);
    }

    private static void logKnownVersion(ChannelHandlerContext ctx, ProxyProtocolEnum version) {
        log.debug("{} Protocol version: {}", ctx.channel(), version);
    }

    private ProxyProtocolEnum parseProxyProtocol(ByteBuf msg) {
        int readerIndex = msg.readerIndex();
        int writerIndex = msg.writerIndex();
        if(writerIndex == readerIndex) {
            return null;
        }
        // HTTPS 22
        // SOCKS ?
        // HTTP 67 //
        byte versionVal = msg.getByte(readerIndex);
        SocksVersion socksVersion = SocksVersion.valueOf(versionVal);
        switch (socksVersion) {
            case SOCKS4a:
                return ProxyProtocolEnum.SOCKS4a;
            case SOCKS5:
                return ProxyProtocolEnum.SOCKS5;
            default:
                return versionVal == 22 ? ProxyProtocolEnum.HTTPS : ProxyProtocolEnum.HTTP;
        }
    }

    public void addHttpSupport(ChannelHandlerContext ctx) {
        ctx.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                .addLast(new HttpProxyServerConnectionHandler(serverConfig));
    }
    public void addHttpsSupport(ChannelHandlerContext ctx) throws SSLException, CertificateException {
        Channel ch = ctx.channel();

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
        ch.pipeline().addAfter(ctx.name(), null, sslCtx.newHandler(ch.alloc()));

        addHttpSupport(ctx);
    }
}
