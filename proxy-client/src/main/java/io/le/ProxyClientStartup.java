package io.le;

import io.le.proxy.client.gui.Settings;
import io.le.proxy.client.gui.TrayGUI;
import io.le.proxy.server.relay.HttpProxyRelayServer;
import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import io.le.proxy.server.relay.config.ReplayRuleConfig;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.Arrays;

@Slf4j
public class ProxyClientStartup {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        try {
            org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();
            // 不显示设置按钮
            UIManager.put("RootPane.setupButtonVisible", false);
        } catch(Exception e) {
            log.warn("Apply skin Exception: " + e.getMessage());
        }

        Settings settings = new Settings();
        TrayGUI trayGUI = new TrayGUI(settings);
        // proxySystem();
    }

    private static void proxySystem() {
        // 中继代理
        HttpProxyRelayServer httpRelayProxyServer = new HttpProxyRelayServer();
        HttpProxyRelayServerConfig httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();
        httpProxyRelayServerConfig.setCodecSsl(false);
        httpProxyRelayServerConfig.setProxyProtocol(HttpProxyServerConfig.ProxyProtocol.HTTP);
        httpProxyRelayServerConfig.setRelayProtocol(HttpProxyServerConfig.ProxyProtocol.LEE);
        // httpProxyRelayServerConfig.setRealProxyHost("74.91.26.90");
        httpProxyRelayServerConfig.setRealProxyHost("8.210.18.42");
        httpProxyRelayServerConfig.setRealProxyPort(40000);
        // httpProxyRelayServerConfig.setProxyHost("sprint.ikeatw.com");
        // httpProxyRelayServerConfig.setProxyPort(50000);
        // httpProxyRelayServerConfig.setProxyUsername("sprint");
        // httpProxyRelayServerConfig.setProxyPassword("FtMM7EvG");

        httpProxyRelayServerConfig.setPort(40001);
        httpProxyRelayServerConfig.setBossGroupThreads(5);
        httpProxyRelayServerConfig.setWorkerGroupThreads(10);

        ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
        replayRuleConfig.setDirectHosts(Arrays.asList("ip111", "baidu.com"));
        httpProxyRelayServerConfig.setReplayFilterConfig(replayRuleConfig);
        httpRelayProxyServer.start(httpProxyRelayServerConfig);
    }
}
