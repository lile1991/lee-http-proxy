package io.le.proxy.server.handler.socks5;

import io.le.proxy.server.config.ProxyServerConfig;
import io.le.proxy.server.handler.ProxyExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5ProxyServerConnectionHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private final ProxyServerConfig serverConfig;
    public Socks5ProxyServerConnectionHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        if(msg.type().equals(Socks5CommandType.CONNECT)) {
            log.trace("Prepare to connect to the target server {}:{}", msg.dstAddr(), msg.dstPort());

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 将目标服务器信息转发给客户端
                            ch.pipeline().addLast(new ProxyExchangeHandler(serverConfig, ctx.channel()));
                        }
                    });
            ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
            future.addListener((ChannelFutureListener) future1 -> {
                Channel clientChannel = future1.channel();
                if(future1.isSuccess()) {
                    log.debug("Successfully connected to {}:{}! \r\n{}", msg.dstAddr(), msg.dstPort(), clientChannel);
                    ctx.pipeline().addLast(new ProxyExchangeHandler(serverConfig, clientChannel));
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));

                    ctx.pipeline().remove(Socks5ServerEncoder.class);
                } else {
                    log.error("Connected failed {}:{}", msg.dstAddr(), msg.dstPort());
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4));
                }
            });

            ctx.pipeline().remove(ctx.name());
        }/* else {
            // never execute
            log.warn("Socks5 channelRead0 {}", msg);
            ctx.fireChannelRead(msg);
        }*/
    }
}