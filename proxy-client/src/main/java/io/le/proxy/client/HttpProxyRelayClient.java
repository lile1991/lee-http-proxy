package io.le.proxy.client;

import io.le.proxy.server.relay.HttpProxyRelayServer;
import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import lombok.Getter;

public class HttpProxyRelayClient {
    @Getter
    private final HttpProxyRelayServer httpProxyRelayServer = new HttpProxyRelayServer();
    @Getter
    private final HttpProxyRelayServerConfig httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();

    public void start() {
        if(httpProxyRelayServer.isRunning()) {
            return;
        }
        httpProxyRelayServer.start(httpProxyRelayServerConfig);
    }
}
