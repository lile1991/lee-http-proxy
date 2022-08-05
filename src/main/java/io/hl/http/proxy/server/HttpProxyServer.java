package io.hl.http.proxy.server;

import io.hl.http.proxy.relay.handler.codec.lee.LeeServerCodec;
import io.hl.http.proxy.server.config.HttpProxyServerConfig;
import io.hl.http.proxy.server.handler.HttpProxyServerConnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpProxyServer {

    /**
     * 启动代理服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public void start(HttpProxyServerConfig serverConfig) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
        log.info("HTTP proxy server bind to port: {}", serverConfig.getPort());
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        switch (serverConfig.getProxyProtocol()) {
                            case LEE: ch.pipeline().addLast(new LeeServerCodec()); break;
                        }

                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                            .addLast(new HttpProxyServerConnectionHandler(serverConfig));
                    }
                }).bind(serverConfig.getPort());
    }
}
