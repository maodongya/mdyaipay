package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.trace.http.OutgoingHttpTraceSupport;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** JDK 网关或未启 Dubbo 时，走 user 内网 HTTP。 */
public final class HttpOpenApiCredentialResolver implements OpenApiCredentialResolver {

    private final HttpClient httpClient;
    private final String userBaseUrl;
    private final ObjectMapper json;

    public HttpOpenApiCredentialResolver(String userBaseUrl, ObjectMapper json) {
        this.userBaseUrl = stripTrailingSlash(userBaseUrl);
        this.json = json;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public ResolveOpenApiCredentialResult resolve(String appKey) throws MerchantSignedCollectException {
        try {
            String encoded = URLEncoder.encode(appKey, StandardCharsets.UTF_8);
            URI uri = URI.create(userBaseUrl + "/internal/v1/open-api/credentials/resolve?appKey=" + encoded);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .header("Accept", "application/json");
            OutgoingHttpTraceSupport.inject(builder);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw MerchantSignedCollectException.credentialFailed("http " + response.statusCode());
            }
            JsonNode root = json.readTree(response.body());
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                throw MerchantSignedCollectException.credentialFailed(root.path("message").asText("credential denied"));
            }
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                throw MerchantSignedCollectException.credentialFailed("empty credential data");
            }
            return json.treeToValue(data, ResolveOpenApiCredentialResult.class);
        } catch (MerchantSignedCollectException ex) {
            throw ex;
        } catch (Exception ex) {
            throw MerchantSignedCollectException.credentialFailed(ex.getMessage());
        }
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:8082";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
