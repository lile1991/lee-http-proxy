package io.le.proxy.server.relay;

import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import io.le.proxy.server.relay.handler.HttpRelayFilterHandler;
import io.le.proxy.server.relay.handler.codec.lee.LeeServerCodec;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpProxyRelayServer {

    /**
     * 启动代理中继服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public void start(HttpProxyRelayServerConfig serverConfig) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
        log.debug("HTTP proxy relay server bind to port: {}", serverConfig.getPort());
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        switch (serverConfig.getProxyProtocol()) {
                            case LEE: ch.pipeline().addLast(new LeeServerCodec()); break;
                        }

                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                            .addLast(new HttpRelayFilterHandler(serverConfig))
                            // .addLast(new HttpRelayConnectionHandler(serverConfig))
                            ;
                    }
                })
                .bind(serverConfig.getPort());
    }
}
