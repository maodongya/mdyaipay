package com.mdyaipay.tools.loadtest.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 target 中 TLS/超时配置缓存 {@link HttpClient}，避免每样本新建客户端。
 * <p>
 * {@code insecure=true} 仅用于测试环境自签证书，生产压测应使用正规 CA。
 */
final class HttpClientFactory {

    private static final ConcurrentHashMap<String, HttpClient> CACHE = new ConcurrentHashMap<>();

    private HttpClientFactory() {
    }

    static HttpClient clientFor(Map<String, Object> target) {
        String key = cacheKey(target);
        return CACHE.computeIfAbsent(key, k -> build(target));
    }

    private static String cacheKey(Map<String, Object> target) {
        return HttpTargetSupport.insecureTls(target) + "|" + HttpTargetSupport.maxConnections(target) + "|"
                + HttpTargetSupport.timeout(target).toMillis();
    }

    private static HttpClient build(Map<String, Object> target) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(HttpTargetSupport.timeout(target))
                .version(HttpClient.Version.HTTP_1_1);
        if (HttpTargetSupport.insecureTls(target)) {
            builder.sslContext(trustAllContext());
        }
        return builder.build();
    }

    private static SSLContext trustAllContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create insecure SSL context", e);
        }
    }
}
