package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {
    private final HttpProxyServerConfig serverConfig;

    public Socks5InitialRequestHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        if(msg.decoderResult().isFailure()) {
            log.error("Illegal ss5 protocol: {}", msg);
            ctx.close();
        } else {
            if(msg.version().equals(SocksVersion.SOCKS5)) {
                if(serverConfig.getUsernamePasswordAuth() != null) {
                    Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
                    ctx.writeAndFlush(initialResponse);
                    // socks auth message decoder
                    ctx.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                    // socks auth
                    ctx.pipeline().addLast(new Socks5PasswordAuthRequestHandler(serverConfig.getUsernamePasswordAuth()));
                } else {
                    Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                    ctx.writeAndFlush(initialResponse);
                }
                ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
                ctx.pipeline().remove(ctx.name());

                //socks connection
                ctx.pipeline().addLast(new Socks5CommandRequestDecoder());
                //Socks connection
                ctx.pipeline().addLast(new Socks5ProxyServerConnectionHandler(serverConfig));
            }
        }
    }
}
