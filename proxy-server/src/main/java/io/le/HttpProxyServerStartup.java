package io.le;

import io.le.http.proxy.relay.HttpProxyRelayServer;
import io.le.http.proxy.relay.config.HttpProxyRelayServerConfig;
import io.le.http.proxy.server.HttpProxyServer;
import io.le.http.proxy.server.config.HttpProxyServerConfig;

public class HttpProxyServerStartup {
    public static void main(String[] args) {
        // demo6789();

        // HTTP代理(不解码HTTPS)
        HttpProxyServer httpProxyServer = new HttpProxyServer();
        HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
        httpProxyServerConfig.setProxyProtocol(HttpProxyServerConfig.ProxyProtocol.LEE);
        httpProxyServerConfig.setCodecSsl(false);
        httpProxyServerConfig.setPort(40000);
        httpProxyServerConfig.setBossGroupThreads(5);
        httpProxyServerConfig.setWorkerGroupThreads(10);
        httpProxyServer.start(httpProxyServerConfig);
    }

    private static void demo6789() {
        {
            // HTTP代理(解码HTTPS)
            HttpProxyServer httpProxyServer = new HttpProxyServer();
            HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
            httpProxyServerConfig.setCodecSsl(true);
            httpProxyServerConfig.setPort(6666);
            httpProxyServerConfig.setBossGroupThreads(5);
            httpProxyServerConfig.setWorkerGroupThreads(10);
            httpProxyServer.start(httpProxyServerConfig);
        }

        {
            // HTTP代理(不解码HTTPS)
            HttpProxyServer httpProxyServer = new HttpProxyServer();
            HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
            httpProxyServerConfig.setCodecSsl(false);
            httpProxyServerConfig.setPort(7777);
            httpProxyServerConfig.setBossGroupThreads(5);
            httpProxyServerConfig.setWorkerGroupThreads(10);
            httpProxyServer.start(httpProxyServerConfig);
        }

        {
            // 中继代理
            HttpProxyRelayServer httpRelayProxyServer = new HttpProxyRelayServer();
            HttpProxyRelayServerConfig httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();
            httpProxyRelayServerConfig.setCodecSsl(true);
            httpProxyRelayServerConfig.setRealProxyHost("127.0.0.1");
            httpProxyRelayServerConfig.setRealProxyPort(6666);
            // httpProxyRelayServerConfig.setProxyHost("sprint.ikeatw.com");
            // httpProxyRelayServerConfig.setProxyPort(50000);
            // httpProxyRelayServerConfig.setProxyUsername("sprint");
            // httpProxyRelayServerConfig.setProxyPassword("FtMM7EvG");

            httpProxyRelayServerConfig.setPort(9999);
            httpProxyRelayServerConfig.setBossGroupThreads(5);
            httpProxyRelayServerConfig.setWorkerGroupThreads(10);
            httpRelayProxyServer.start(httpProxyRelayServerConfig);
        }

        {
            // 中继代理
            HttpProxyRelayServer httpRelayProxyServer = new HttpProxyRelayServer();
            HttpProxyRelayServerConfig httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();
            httpProxyRelayServerConfig.setCodecSsl(false);
            httpProxyRelayServerConfig.setRealProxyHost("127.0.0.1");
            httpProxyRelayServerConfig.setRealProxyPort(7777);
            // httpProxyRelayServerConfig.setProxyHost("sprint.ikeatw.com");
            // httpProxyRelayServerConfig.setProxyPort(50000);
            // httpProxyRelayServerConfig.setProxyUsername("sprint");
            // httpProxyRelayServerConfig.setProxyPassword("FtMM7EvG");

            httpProxyRelayServerConfig.setPort(8888);
            httpProxyRelayServerConfig.setBossGroupThreads(5);
            httpProxyRelayServerConfig.setWorkerGroupThreads(10);
            httpRelayProxyServer.start(httpProxyRelayServerConfig);
        }
    }
}
