package io.le.proxy.server.handler.http.relay.http;

import io.le.proxy.server.config.ProxyProtocolEnum;
import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.ProxyExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 与真实Proxy Server握手处理器
 */
@Slf4j
public class HttpsConnectRelayShakeHandsHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;

    public HttpsConnectRelayShakeHandsHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
    }

    /**
     * 处理真实代理服务器的响应
     * @param ctx 中继连接  relay server -> real proxy server
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpResponse response = (HttpResponse) msg;
        log.debug("Connection status: {}", response.status());

        if(HttpResponseStatus.OK.code() == response.status().code()) {
            proxyServerChannel.writeAndFlush(response, proxyServerChannel.newPromise().addListener(f -> {
                // 中继都不解码消息， 移除代理服务器的解码器
                proxyServerChannel.pipeline().remove(HttpServerCodec.class);
                proxyServerChannel.pipeline().remove(HttpObjectAggregator.class);
                proxyServerChannel.pipeline().addLast(new ProxyExchangeHandler(serverConfig, ctx.channel()));
                log.debug("Remove HttpServerCodec from pipeline");

                ctx.pipeline().remove(ctx.name());
                ctx.pipeline().remove(HttpClientCodec.class);
                ctx.pipeline().remove(HttpObjectAggregator.class);
//                if(serverConfig.getRelayServerConfig().getRelayProtocol() == ProxyProtocolEnum.HTTPS) {
//                    ctx.pipeline().remove(SslHandler.class);
//                }
                ctx.pipeline().addLast(new ProxyExchangeHandler(serverConfig, proxyServerChannel));
                log.debug("Add ProxyExchangeHandler to ProxyServerPipeline and RelayServerPipeline.");
            }));
        } else {
            proxyServerChannel.writeAndFlush(response).addListener(f -> {
                proxyServerChannel.close();
                ctx.close();
            });
        }
    }
}
