package io.le.proxy.server.relay.config;

import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.config.ProxyProtocolEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
public class HttpProxyRelayServerConfig extends HttpProxyServerConfig {
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
