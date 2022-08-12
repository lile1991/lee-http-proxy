package io.le.proxy.server.relay.config;

import io.le.proxy.server.server.config.ProxyProtocolEnum;
import io.le.proxy.server.server.config.ProxyServerConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
public class HttpProxyRelayServerConfig extends ProxyServerConfig {
    /** */
    private String realProxyHost;
    private int realProxyPort;


    private String proxyUsername;
    private String proxyPassword;

    /** 中继协议 */
    private List<ProxyProtocolEnum> relayProtocols = Collections.singletonList(ProxyProtocolEnum.HTTP);

    /** 中继器规则配置 */
    private ReplayRuleConfig replayRuleConfig;
}
