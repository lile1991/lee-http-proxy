package io.le.proxy.client.gui;

import io.le.proxy.server.relay.config.HttpProxyRelayServerConfig;
import io.le.proxy.server.relay.config.ReplayRuleConfig;
import io.le.proxy.server.server.config.HttpProxyServerConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Settings {
    private final JFrame frame;

    HttpProxyRelayServerConfig httpProxyRelayServerConfig;
    public Settings() {

        httpProxyRelayServerConfig = new HttpProxyRelayServerConfig();
        httpProxyRelayServerConfig.setCodecSsl(false);
        httpProxyRelayServerConfig.setProxyProtocol(HttpProxyServerConfig.ProxyProtocol.HTTP);
        httpProxyRelayServerConfig.setRelayProtocol(HttpProxyServerConfig.ProxyProtocol.LEE);

        ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
        httpProxyRelayServerConfig.setReplayFilterConfig(replayRuleConfig);

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

        jPanel.setLayout(new GridLayout(5, 2));
        {
            JLabel jLabel = new JLabel("Proxy node");
            JComboBox<String> jComboBox = new JComboBox<>();
            jComboBox.addItem("HK");
            jComboBox.addItem("US");

            jPanel.add(jLabel);
            jPanel.add(jComboBox);
        }

        {
            JLabel jLabel = new JLabel("Proxy rule");
            JComboBox<String> jComboBox = new JComboBox<>();
            jComboBox.addItem(ReplayRuleConfig.ProxyMode.DEFAULT.desc);
            jComboBox.addItem(ReplayRuleConfig.ProxyMode.ONLY_PROXY.desc);

            jPanel.add(jLabel);
            jPanel.add(jComboBox);
        }

        {
            JLabel jLabel = new JLabel("Proxy hosts");
            JTextArea jTextArea = new JTextArea();
            JScrollPane jScrollPane=new JScrollPane(jTextArea);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jPanel.add(jLabel);
            jPanel.add(jTextArea);
        }
        {
            JLabel jLabel = new JLabel("Do not proxy hosts");
            JTextArea jTextArea = new JTextArea();
            JScrollPane jScrollPane=new JScrollPane(jTextArea);
            jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jPanel.add(jLabel);
            jPanel.add(jTextArea);
        }

        {
            JLabel jLabel = new JLabel("");
            JButton jButton = new JButton("Apply");

            jPanel.add(jLabel);
            jPanel.add(jButton);
        }
        frame.add(jPanel);
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
    public boolean isVisible() {
        return frame.isVisible();
    }
}
