package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5ProxyServerConnectionHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private final HttpProxyServerConfig serverConfig;
    public Socks5ProxyServerConnectionHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
        log.debug("目标服务器  : " + msg.type() + "," + msg.dstAddr() + "," + msg.dstPort());
        if(msg.type().equals(Socks5CommandType.CONNECT)) {
            log.trace("准备连接目标服务器");

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //ch.pipeline().addLast(new LoggingHandler());//in out
                            //将目标服务器信息转发给客户端
                            ch.pipeline().addLast(new Socks5ServerDispatcherHandler(serverConfig, ctx.channel()));
                        }
                    });
            log.trace("连接目标服务器");
            ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
            future.addListener(new ChannelFutureListener() {

                public void operationComplete(final ChannelFuture future) throws Exception {
                    Channel clientChannel = future.channel();
                    if(future.isSuccess()) {
                        log.debug("{} Successfully connected to {}:{}!", clientChannel, msg.dstAddr(), msg.dstPort());
                        ctx.pipeline().addLast(new Socks5ProxyClientDispatcherHandler(serverConfig, clientChannel));
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                        ctx.writeAndFlush(commandResponse);
                    } else {
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                        ctx.writeAndFlush(commandResponse);
                    }
                }

            });
        } else {
            log.warn("Socks5 channelRead0 {}", msg);
            ctx.fireChannelRead(msg);
        }
    }
}
