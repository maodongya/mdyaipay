package com.mdyaipay.gateway;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** 将商户相关请求转发至 user 服务 HTTP（默认 8082）。 */
final class UserBackendClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    UserBackendClient(String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    PaymentBackendClient.BackendResponse forward(String method, String path, String body)
            throws IOException, InterruptedException {
        URI uri = URI.create(baseUrl + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json; charset=utf-8");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body != null ? body : "", StandardCharsets.UTF_8));
        }
        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new PaymentBackendClient.BackendResponse(response.statusCode(), response.body());
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:8082";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
