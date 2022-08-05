package io.hl.http.proxy.relay.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReplayRuleConfig {
    private FilterMode filterMode = FilterMode.ONLY_DIRECT_TARGET;
    // 直连
    private List<String> directHosts;

    // 代理
    private List<String> proxyHosts;

    public enum FilterMode {
        ONLY_PROXY_TARGET,
        ONLY_DIRECT_TARGET,
    }
}
