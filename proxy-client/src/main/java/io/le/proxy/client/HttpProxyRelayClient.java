package io.ml.proxy.client;

import io.ml.proxy.server.relay.HttpProxyRelayServer;
import io.ml.proxy.server.relay.config.HttpProxyRelayServerConfig;
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
