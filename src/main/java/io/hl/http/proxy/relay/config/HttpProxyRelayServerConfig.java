package io.hl.http.proxy.relay.config;

import io.hl.http.proxy.server.config.HttpProxyServerConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class HttpProxyRelayServerConfig extends HttpProxyServerConfig {
    /** */
    private String realProxyHost;
    private int realProxyPort;


    private String proxyUsername;
    private String proxyPassword;

    /** 中继协议 */
    private ProxyProtocol relayProtocol = ProxyProtocol.HTTP;

}
