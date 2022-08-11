package io.le.proxy.server.server.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
public class HttpProxyServerConfig {
    /** 绑定的端口 */
    protected int port;

    /** 是否解码HTTPS数据 */
    protected boolean codecSsl = false;

    /** HTTP合包的最大大小 15MB */
    protected int httpObjectAggregatorMaxContentLength = 15 * 1024 * 128;

    protected int bossGroupThreads;
    protected int workerGroupThreads;

    private SocketAddress localAddress;

    /** 代理协议 */
    protected List<ProxyProtocolEnum> proxyProtocols = Collections.singletonList(ProxyProtocolEnum.HTTP);

}
