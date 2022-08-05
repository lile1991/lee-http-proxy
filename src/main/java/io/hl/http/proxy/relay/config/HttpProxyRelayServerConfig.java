package io.hl.http.proxy.relay.config;

import io.hl.http.proxy.server.config.HttpProxyServerConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
