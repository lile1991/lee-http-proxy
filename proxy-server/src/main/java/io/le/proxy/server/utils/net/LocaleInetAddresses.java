package io.le.proxy.server.utils.net;

import io.le.proxy.server.utils.io.IOUtils;
import io.le.proxy.server.utils.lang.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public class LocaleInetAddresses {

    @Getter
    private static InetAddress[] inetAddresses;

    static {
        try (InputStream inputStream = LocaleInetAddresses.class.getResourceAsStream("/ip_addresses.txt")) {
            String ips = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inetAddresses = Arrays.stream(ips.split("\\s+")).map(String::trim).filter(StringUtils::isNotBlack).map(ip -> {
                try {
                    return InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    log.error("ip=" + ip, e);
                }
                return null;
            }).filter(Objects::nonNull).toArray(InetAddress[]::new);
            log.debug("成功加载到IP列表: {}", Arrays.asList(inetAddresses));
        } catch (Exception e) {
            log.error("未读取到IP配置， 使用默认IP: {}", e.getMessage());
        }
    }

    public static InetAddress next() {
        return inetAddresses == null ? null : inetAddresses[(int) (System.currentTimeMillis() % inetAddresses.length)];
    }
}
