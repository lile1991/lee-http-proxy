package io.le.proxy.server.handler.http.relay;

import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5PasswordAuthResponseHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;
    public Socks5PasswordAuthResponseHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Socks5PasswordAuthResponse response = (Socks5PasswordAuthResponse) msg;
        if(response.status() == Socks5PasswordAuthStatus.SUCCESS) {
            log.debug("Socks5 auth success");

            log.debug("Add Socks5CommandResponseHandler to relay server pipeline.");
            // 增加CommandHandler
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseDecoder());

            // 鉴权请求Handler不要了
            ctx.pipeline().remove(Socks5PasswordAuthResponseDecoder.class);
            ctx.pipeline().remove(ctx.name());

            // log.debug("Add ProxyExchangeHandler to relay server pipeline.");
            // ctx.pipeline().addAfter(ctx.name(), null, new ProxyExchangeHandler(serverConfig, proxyServerChannel));

            // log.debug("Add ProxyExchangeHandler to proxy server pipeline.");
            // proxyServerChannel.pipeline().addLast(new ProxyExchangeHandler(serverConfig, ctx.channel()));

            // ctx.pipeline().remove(Socks5ClientEncoder.class);

            DefaultSocks5CommandRequest socks5CommandRequest = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                    Socks5AddressType.DOMAIN, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
            ctx.writeAndFlush(socks5CommandRequest);
        } else {
            log.debug("Socks5 auth failure");
        }
    }
}
