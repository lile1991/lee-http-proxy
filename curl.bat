# --insecure跳过https请求证书校验
curl --insecure -v -x http://127.0.0.1:40002 https://ipinfo.io

# --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x  https://127.0.0.1:40002 https://ipinfo.io -k