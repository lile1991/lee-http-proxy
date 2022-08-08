package io.le;

import io.le.proxy.client.HttpProxyRelayClient;
import io.le.proxy.client.gui.TrayGUI;
import io.le.proxy.client.gui.settings.Settings;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

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

        HttpProxyRelayClient httpProxyRelayClient = new HttpProxyRelayClient();
        Settings settings = new Settings(httpProxyRelayClient);
        TrayGUI trayGUI = new TrayGUI(settings);
        settings.setTrayGUI(trayGUI);
    }
}
