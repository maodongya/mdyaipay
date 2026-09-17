package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.model.ApiResponse;
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
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResponse<ResolveOpenApiCredentialResult> body = json.readValue(
                    response.body(), new TypeReference<>() {});
            if (response.statusCode() != 200 || body == null || body.getCode() != 0 || body.getData() == null) {
                String msg = body != null ? body.getMessage() : "http " + response.statusCode();
                throw MerchantSignedCollectException.credentialFailed(msg);
            }
            return body.getData();
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
