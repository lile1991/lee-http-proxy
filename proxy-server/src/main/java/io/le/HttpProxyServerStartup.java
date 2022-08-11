package io.le;

import io.le.proxy.server.relay.HttpProxyRelayServer;
import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import io.le.proxy.server.server.HttpProxyServer;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.server.config.ProxyProtocolEnum;
import io.le.proxy.server.utils.net.LocaleInetAddresses;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

public class HttpProxyServerStartup {
    public static void main(String[] args) {
        // demo6789();

        // HTTP代理(不解码HTTPS)
        {
            HttpProxyServer httpProxyServer = new HttpProxyServer();
            HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
            httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                    ProxyProtocolEnum.HTTPS,
                    ProxyProtocolEnum.SOCKS4a,
                    ProxyProtocolEnum.SOCKS5));
            httpProxyServerConfig.setCodecSsl(false);
            httpProxyServerConfig.setPort(40000);
            httpProxyServerConfig.setBossGroupThreads(5);
            httpProxyServerConfig.setWorkerGroupThreads(10);
            httpProxyServer.start(httpProxyServerConfig);
        }

        // 多IP出口
        InetAddress[] inetAddresses = LocaleInetAddresses.getInetAddresses();
        if(inetAddresses != null) {
            Random random = new Random();
            for (int i = 0, inetAddressesLength = inetAddresses.length; i < inetAddressesLength; i++) {
                InetAddress inetAddress = inetAddresses[i];
                HttpProxyServer httpProxyServer = new HttpProxyServer();
                HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
                httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP, ProxyProtocolEnum.HTTPS, ProxyProtocolEnum.LEE));
                httpProxyServerConfig.setCodecSsl(false);
                httpProxyServerConfig.setPort(40001 + i);
                httpProxyServerConfig.setBossGroupThreads(5);
                httpProxyServerConfig.setWorkerGroupThreads(10);
                httpProxyServerConfig.setLocalAddress(new InetSocketAddress(inetAddress, 45000 + random.nextInt(5000)));
                httpProxyServer.start(httpProxyServerConfig);
            }
        } else {
            HttpProxyServer httpProxyServer = new HttpProxyServer();
            HttpProxyServerConfig httpProxyServerConfig = new HttpProxyServerConfig();
            httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP, ProxyProtocolEnum.HTTPS, ProxyProtocolEnum.LEE));
            httpProxyServerConfig.setCodecSsl(false);
            httpProxyServerConfig.setPort(40001);
            httpProxyServerConfig.setBossGroupThreads(5);
            httpProxyServerConfig.setWorkerGroupThreads(10);
            httpProxyServer.start(httpProxyServerConfig);
        }
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
