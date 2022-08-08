package io.le.proxy.client.config;

import io.le.proxy.server.config.ConfReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class SettingsConfReader {
    final File configPath = new File(new File("config").getAbsolutePath());

    final File confFile = new File(configPath, "minusone.conf");

    public Map<String, Object> readFile() throws IOException {
        return ConfReader.readFile(confFile, Arrays.asList(
                new ConfReader.PropertyDefined("main"),
                new ConfReader.PropertyDefined("proxy-hosts", true),
                new ConfReader.PropertyDefined("direct-hosts", true)
        ));
    }
}
