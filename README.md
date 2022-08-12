## Lee Http Proxy
Support relay mode
```java
// 支持HTTP、HTTPS、SOCKS5代理协议, 自动识别
HttpProxyServer httpProxyServer = new HttpProxyServer();
ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
        ProxyProtocolEnum.HTTPS,
        ProxyProtocolEnum.SOCKS4a,
        ProxyProtocolEnum.SOCKS5));
httpProxyServerConfig.setCodecMsg(false);
httpProxyServerConfig.setPort(40000);
httpProxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "123123"));
httpProxyServerConfig.setBossGroupThreads(5);
httpProxyServerConfig.setWorkerGroupThreads(10);

httpProxyServer.start(httpProxyServerConfig);
```

测试
```shell
# --insecure 跳过https请求证书校验
curl --insecure -v -x http://127.0.0.1:40000 https://ipinfo.io

# --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x https://127.0.0.1:40000 https://ipinfo.io -k

# socks5代理
curl -v -x socks5://127.0.0.1:40000 https://ipinfo.io
```