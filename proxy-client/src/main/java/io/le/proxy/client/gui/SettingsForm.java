package io.le.proxy.client.gui;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class SettingsForm {

    @Setter
    @Getter
    static class Form {
        private String proxyNode;
        private String proxyMode;
        private String proxyHosts;
        private String doNotProxyHosts;
    }

    Form form = new Form();
    JComboBox<String> proxyNodeComboBox;
    JComboBox<String> proxyModeComboBox;
    JTextArea proxyHostsTextArea;
    JTextArea doNotProxyHostsTextArea;

    public Form get() {
        String proxyNode = (String) proxyNodeComboBox.getSelectedItem();
        String proxyMode = (String) proxyModeComboBox.getSelectedItem();
        String proxyHosts = proxyHostsTextArea.getText();
        String doNotProxyHosts = doNotProxyHostsTextArea.getText();

        form.setProxyNode(proxyNode);
        form.setProxyMode(proxyMode);
        form.setProxyHosts(proxyHosts);
        form.setDoNotProxyHosts(doNotProxyHosts);
        return form;
    }
}
