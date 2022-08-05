package io.le.proxy.server.relay.config;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HttpProxyRelayServerConfig extends HttpProxyServerConfig {
    /** */
    private String realProxyHost;
    private int realProxyPort;


    private String proxyUsername;
    private String proxyPassword;

    /** 中继协议 */
    private ProxyProtocol relayProtocol = ProxyProtocol.HTTP;

    /** 中继器过滤配置 */
    private ReplayRuleConfig replayFilterConfig;
}