package io.le.proxy.server.relay.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReplayRuleConfig {
    private ProxyMode filterMode = ProxyMode.DEFAULT;
    // 直连
    private List<String> directHosts;

    // 代理
    private List<String> proxyHosts;

    @AllArgsConstructor
    public enum ProxyMode {
        // 只代理白名单
        ONLY_PROXY("ONLY_PROXY", "only proxy"),
        // 默认
        DEFAULT("DEFAULT", "default");

        public final String value;
        public final String desc;
    }
}
