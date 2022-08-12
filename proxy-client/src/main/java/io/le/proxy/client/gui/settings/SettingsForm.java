package io.le.proxy.client.gui.settings;

import io.le.proxy.client.config.SettingsConfReader;
import io.le.proxy.utils.io.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
public class SettingsForm {

    @Setter
    @Getter
    static class Form {
        private String proxyNode;
        private String proxyMode;
        private String proxyHosts;
        private String directHosts;
    }

    Form form = new Form();
    JComboBox<String> proxyNodeComboBox;
    JComboBox<String> proxyModeComboBox;
    JTextArea proxyHostsTextArea;
    JTextArea directHostsTextArea;

    public Form get() {
        String proxyNode = (String) proxyNodeComboBox.getSelectedItem();
        String proxyMode = (String) proxyModeComboBox.getSelectedItem();
        String proxyHosts = proxyHostsTextArea.getText();
        String directHosts = directHostsTextArea.getText();

        form.setProxyNode(proxyNode);
        form.setProxyMode(proxyMode);
        form.setProxyHosts(proxyHosts);
        form.setDirectHosts(directHosts);
        return form;
    }


    public void readConfig() {
        try {
            SettingsConfReader settingsConfReader = new SettingsConfReader();
            Map<String, Object> confMap = settingsConfReader.readFile();

            if (confMap != null) {
                Object proxyNode = confMap.get("proxy-node");
                form.setProxyNode(proxyNode == null ? null : proxyNode.toString());

                Object proxyMode = confMap.get("proxy-mode");
                form.setProxyMode(proxyMode == null ? null : proxyMode.toString());

                Object proxyHosts = confMap.get("proxy-hosts");
                form.setProxyHosts(proxyHosts == null ? null : proxyHosts.toString());

                Object directHosts = confMap.get("direct-hosts");
                form.setDirectHosts(directHosts == null ? null : directHosts.toString());
            }
        } catch (IOException e) {
            log.error("Read configuration error: ", e);
        }
    }
    public void saveConfig() {
        File configPath = new File(new File("config").getAbsolutePath());

        File mainConfig = new File(configPath, "minusone.conf");
        java.util.List<String> mainConfigLine = new ArrayList<>();
        mainConfigLine.add("[main]");
        mainConfigLine.add("proxy-node=" + form.getProxyNode());
        mainConfigLine.add("proxy-mode=" + form.getProxyMode());

        mainConfigLine.add("\r\n");
        mainConfigLine.add("[proxy-hosts]");
        mainConfigLine.add(form.getProxyHosts());

        mainConfigLine.add("\r\n");
        mainConfigLine.add("[direct-hosts]");
        mainConfigLine.add(form.getDirectHosts());
        try {
            FileUtils.writeLines(mainConfig, "UTF-8", mainConfigLine, false);
        } catch (IOException e) {
            log.error("Save configuration error: ", e);
        }
    }
}
