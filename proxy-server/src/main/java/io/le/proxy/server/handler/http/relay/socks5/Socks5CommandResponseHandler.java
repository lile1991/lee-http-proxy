package io.le.proxy.server.handler.http.relay.socks5;

import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.ProxyExchangeHandler;
import io.le.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5CommandResponseHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;
    public Socks5CommandResponseHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("channelRead Socks5CommandResponse {}.\r\n{}", msg, ctx);
        DefaultSocks5CommandResponse socks5Command = (DefaultSocks5CommandResponse) msg;
        if(socks5Command.decoderResult() == DecoderResult.SUCCESS) {
            // log.debug("Add ProxyExchangeHandler to relay client pipeline.");
            // ctx.pipeline().addAfter(ctx.name(), null, new ProxyExchangeHandler(serverConfig, proxyServerChannel));

            log.debug("Add ProxyExchangeHandler to proxy server pipeline.");
            proxyServerChannel.pipeline().addLast(new ProxyExchangeHandler(serverConfig, ctx.channel()));

            // ctx.pipeline().remove(Socks5ClientEncoder.class);
            log.debug("Add HttpClientCodec to relay client pipeline.");
            ctx.pipeline().addAfter(ctx.name(), null, new HttpClientCodec());
            ctx.pipeline().addAfter(ctx.name(), null, new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));

            ctx.pipeline().remove(Socks5CommandResponseDecoder.class);
            ctx.pipeline().remove(ctx.name());

            log.debug("Write http request to remote.\r\n{}", ctx);
            ctx.writeAndFlush(httpRequestInfo);
        } else {
            log.error("decoderResult is {}, close channel", socks5Command.decoderResult());
            ctx.close();
            proxyServerChannel.close();
        }
    }
}
