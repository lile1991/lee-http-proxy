package io.le.proxy.server.server;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.handler.ProxyUnificationServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

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
        log.debug("HTTP proxy server bind to port: {}, protocol: {}", serverConfig.getPort(), serverConfig.getProxyProtocols());
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws CertificateException, SSLException {
                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                            .addLast(new ProxyUnificationServerHandler(serverConfig));
                    }
                }).bind(serverConfig.getPort());
    }
}
