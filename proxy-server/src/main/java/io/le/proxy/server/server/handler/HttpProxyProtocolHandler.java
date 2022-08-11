package io.le.proxy.server.server.handler;

import io.le.proxy.server.relay.handler.codec.lee.LeeServerCodec;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.config.ProxyProtocolEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.SocketAddress;
import java.security.cert.CertificateException;

/**
 * 用于同时支持 HTTP、HTTPS、SOCKS4、SOCKS5 代理协议
 */
@Slf4j
public class HttpProxyProtocolHandler extends ChannelInboundHandlerAdapter {

    private HttpProxyServerConfig serverConfig;

    public HttpProxyProtocolHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // HTTPS 22
        // HTTP 67
        // SOCKS 67
        System.out.println(((ByteBuf) msg).getByte(0));
        ProxyProtocolEnum protocol = parseProxyProtocol((ByteBuf) msg);
        if(protocol == null) {
            return;
        }
        switch (protocol) {
            case HTTPS: addHttpsSupport(ctx); break;
            case SOCKS5: break;
        }

        if (serverConfig.getProxyProtocols().contains(ProxyProtocolEnum.LEE)) {
            ctx.pipeline().addLast(new LeeServerCodec());
        }

        ctx.pipeline().remove(getClass());
        super.channelRead(ctx, msg);
    }

    private ProxyProtocolEnum parseProxyProtocol(ByteBuf msg) {
        int readerIndex = msg.readerIndex();
        int writerIndex = msg.writerIndex();
        if(writerIndex == readerIndex) {
            return null;
        }
        byte firstByte = msg.getByte(readerIndex);
        return firstByte == 22 ? ProxyProtocolEnum.HTTPS : ProxyProtocolEnum.HTTP;
    }

    public void addHttpsSupport(ChannelHandlerContext ctx) throws SSLException, CertificateException {
        Channel ch = ctx.channel();
        if(!serverConfig.getProxyProtocols().contains(ProxyProtocolEnum.HTTPS)) {
            log.warn("{} not support https proxy", ch);
            return;
        }

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
        ch.pipeline().addAfter(ctx.name(), "", sslCtx.newHandler(ch.alloc()));
    }
}
