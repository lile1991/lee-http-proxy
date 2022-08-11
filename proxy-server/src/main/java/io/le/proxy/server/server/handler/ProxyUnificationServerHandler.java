package io.le.proxy.server.server.handler;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.config.ProxyProtocolEnum;
import io.le.proxy.server.server.handler.http.HttpProxyServerConnectionHandler;
import io.le.proxy.server.server.handler.https.SslHandlerCreator;
import io.le.proxy.server.server.handler.socks5.Socks5InitialRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

        logKnownVersion(ctx, protocol);

        if(!serverConfig.getProxyProtocols().contains(protocol)) {
            log.warn("The proxy server does not support the {} protocol!", protocol);
            ctx.close();
            return;
        }

        ChannelPipeline p = ctx.pipeline();
        switch (protocol) {
            case HTTP: addHttpHandles(ctx); break;
            case HTTPS: addHttpsHandlers(ctx); break;
            case SOCKS4a:
                p.addAfter(ctx.name(), null, Socks4ServerEncoder.INSTANCE);
                p.addAfter(ctx.name(), null, new Socks4ServerDecoder());
                break;
            case SOCKS5:
                addSocks5Handler(ctx); break;
            default:
                ctx.write(ctx.alloc().buffer().writeCharSequence("Proxy protocol not supported.", StandardCharsets.UTF_8))
                        .addListener(ChannelFutureListener.CLOSE);
                return;
        }

        /*if (serverConfig.getProxyProtocols().contains(ProxyProtocolEnum.LEE)) {
            p.addBefore(ctx.name(), null, new LeeServerCodec());
        }*/

        ctx.pipeline().remove(getClass());
        super.channelRead(ctx, msg);
    }

    private void addSocks5Handler(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        ChannelPipeline p = ctx.pipeline();

        // Socks5MessageByteBuf
        p.addAfter(ctx.name(), null, Socks5ServerEncoder.DEFAULT);
        // sock5 init
        p.addAfter(ctx.name(), null, new Socks5InitialRequestDecoder());
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialRequestHandler(serverConfig));
        /*if(isAuth()) {
            //socks auth
            ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
            //socks auth
            ch.pipeline().addLast(new Socks5PasswordAuthRequestHandler(getPasswordAuth()));
        }*/
        // // socks connection
        // ch.pipeline().addLast(new Socks5CommandRequestDecoder());
        // // Socks connection
        // ch.pipeline().addLast(new Socks5CommandRequestHandler(ProxyServer.this.getBossGroup()));
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
        // HTTP 67
        // HTTPS 22
        // SOCKS4 4
        // SOCKS5 5
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

    public void addHttpHandles(ChannelHandlerContext ctx) {
        ctx.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                .addLast(new HttpProxyServerConnectionHandler(serverConfig));
    }
    public void addHttpsHandlers(ChannelHandlerContext ctx) throws SSLException, CertificateException {
        Channel ch = ctx.channel();

        // Support HTTPS proxy protocol
        ch.pipeline().addAfter(ctx.name(), null, SslHandlerCreator.forServer(ch.alloc()));

        // 和HTTP代理协议共用一样的Handler
        addHttpHandles(ctx);
    }
}
