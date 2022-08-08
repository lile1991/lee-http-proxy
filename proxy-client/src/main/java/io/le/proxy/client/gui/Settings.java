package io.le.proxy.client.gui;

import io.le.proxy.client.HttpProxyRelayClient;
import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import io.le.proxy.server.relay.config.ReplayRuleConfig;
import io.le.proxy.server.server.config.HttpProxyServerConfig;
import io.le.proxy.server.utils.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

public class Settings {
    private final HttpProxyRelayClient httpProxyRelayClient;
    private final JFrame frame;

    HttpProxyRelayServerConfig httpProxyRelayServerConfig;
    public Settings(HttpProxyRelayClient httpProxyRelayClient) {
        this.httpProxyRelayClient = httpProxyRelayClient;
        httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();
        httpProxyRelayServerConfig.setCodecSsl(false);
        httpProxyRelayServerConfig.setProxyProtocol(HttpProxyServerConfig.ProxyProtocol.HTTP);
        httpProxyRelayServerConfig.setRelayProtocol(HttpProxyServerConfig.ProxyProtocol.LEE);

        ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
        httpProxyRelayServerConfig.setReplayRuleConfig(replayRuleConfig);

        frame = new JFrame("Minus one");
        Toolkit toolkit = Toolkit.getDefaultToolkit(); // 获取Toolkit对象
        frame.setIconImage(toolkit.getImage(getClass().getResource("/favicon.png").getPath())); // 设置图标

        // 设置大小
        frame.setSize(300, 260);
        // 居中显示
        frame.setLocationRelativeTo(null);
        addSettingField();

        frame.setResizable(false);
        frame.setVisible(true);

        frame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if(e.getNewState() == 1 || e.getNewState() == 7) {
                    // minimize
                    frame.setState(0);
                    frame.setVisible(false);
                }else if(e.getNewState() == 0) {
                    // normal
                }else if(e.getNewState() == 6) {
                    // maximize
                }
            }
        });
    }

    private void addSettingField() {
        JPanel jPanel = new JPanel();

        SettingsForm settingsForm = new SettingsForm();
        jPanel.setLayout(new GridLayout(5, 2));
        {
            JLabel jLabel = new JLabel("Proxy node");
            settingsForm.proxyNodeComboBox = new JComboBox<>();
            settingsForm.proxyNodeComboBox.addItem("HK");
            settingsForm.proxyNodeComboBox.addItem("US");

            jPanel.add(jLabel);
            jPanel.add(settingsForm.proxyNodeComboBox);
        }

        {
            JLabel jLabel = new JLabel("Proxy mode");
            settingsForm.proxyModeComboBox = new JComboBox<>();
            settingsForm.proxyModeComboBox.addItem(ReplayRuleConfig.ProxyMode.DEFAULT.desc);
            settingsForm.proxyModeComboBox.addItem(ReplayRuleConfig.ProxyMode.ONLY.desc);

            jPanel.add(jLabel);
            jPanel.add(settingsForm.proxyModeComboBox);
        }

        {
            JLabel jLabel = new JLabel("Proxy hosts");
            settingsForm.proxyHostsTextArea = new JTextArea();
            JScrollPane jScrollPane=new JScrollPane(settingsForm.proxyHostsTextArea);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jPanel.add(jLabel);
            jPanel.add(settingsForm.proxyHostsTextArea);
        }
        {
            JLabel jLabel = new JLabel("Do not proxy hosts");
            settingsForm.doNotProxyHostsTextArea = new JTextArea();
            JScrollPane jScrollPane=new JScrollPane(settingsForm.doNotProxyHostsTextArea);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jPanel.add(jLabel);
            jPanel.add(settingsForm.doNotProxyHostsTextArea);
        }

        {
            JButton update = new JButton("Update");
            JButton start = new JButton("Start");

            jPanel.add(update);
            jPanel.add(start);

            update.addActionListener(e -> {
                SettingsForm.Form form = settingsForm.get();
                updateRelayServerConfig(httpProxyRelayServerConfig, form);
            });

            start.addActionListener(e -> {
                start.setEnabled(false);
                // Save Settings

                SettingsForm.Form form = settingsForm.get();

                HttpProxyRelayServerConfig httpProxyRelayServerConfig = httpProxyRelayClient.getHttpProxyRelayServerConfig();
                httpProxyRelayServerConfig.setCodecSsl(false);
                httpProxyRelayServerConfig.setProxyProtocol(HttpProxyServerConfig.ProxyProtocol.HTTP);
                httpProxyRelayServerConfig.setRelayProtocol(HttpProxyServerConfig.ProxyProtocol.LEE);

                httpProxyRelayServerConfig.setReplayRuleConfig(new ReplayRuleConfig());
                updateRelayServerConfig(httpProxyRelayServerConfig, form);
                httpProxyRelayClient.start();
                // Notify proxy server
            });
        }
        frame.add(jPanel);
    }

    private void updateRelayServerConfig(HttpProxyRelayServerConfig httpProxyRelayServerConfig, SettingsForm.Form form) {
        switch (form.getProxyNode()) {
            case "HK":
                httpProxyRelayServerConfig.setRealProxyHost("8.210.18.42");
                httpProxyRelayServerConfig.setRealProxyPort(40000);
                break;
            case "US":
                httpProxyRelayServerConfig.setRealProxyHost("74.91.26.90");
                httpProxyRelayServerConfig.setRealProxyPort(40000);
                break;
        }
        httpProxyRelayServerConfig.setPort(40001);
        httpProxyRelayServerConfig.setBossGroupThreads(5);
        httpProxyRelayServerConfig.setWorkerGroupThreads(10);

        ReplayRuleConfig replayRuleConfig = httpProxyRelayServerConfig.getReplayRuleConfig();
        ReplayRuleConfig.ProxyMode proxyModeEnum = ReplayRuleConfig.ProxyMode.enumOfDesc(form.getProxyMode());
        replayRuleConfig.setProxyMode(proxyModeEnum);
        if(StringUtils.isNotBlack(form.getProxyHosts())) {
            replayRuleConfig.setProxyHosts(Arrays.asList(form.getProxyHosts().split("[\\s,，]")));
        }
        if(StringUtils.isNotBlack(form.getDoNotProxyHosts())) {
            replayRuleConfig.setDirectHosts(Arrays.asList(form.getDoNotProxyHosts().split("[\\s,，]")));
        }

    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
    public boolean isVisible() {
        return frame.isVisible();
    }
}
