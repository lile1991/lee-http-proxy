package io.hl.http.proxy.server.config;

import lombok.Data;

@Data
public class HttpProxyServerConfig {
    /** 绑定的端口 */
    protected int port;

    /** 是否解码HTTPS数据 */
    protected boolean codecSsl = false;

    /** HTTP合包的最大大小 15MB */
    protected int httpObjectAggregatorMaxContentLength = 15 * 1024 * 128;

    protected int bossGroupThreads;
    protected int workerGroupThreads;

    /** 代理协议 */
    protected ProxyProtocol proxyProtocol = ProxyProtocol.HTTP;

    public enum ProxyProtocol {
        HTTP, LEE
    }
}
