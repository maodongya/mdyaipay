package com.mdyaipay.tools.loadtest.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.merchant.MerchantOpenApiPayloadCipher;
import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 构造网关对外收单加密报文，与 {@code mdyaipay-gateway} {@code MerchantSignedCollectProcessor} 规则一致。
 */
public final class MerchantEncryptedCollectBodyBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MerchantEncryptedCollectBodyBuilder() {
    }

    public static String build(Map<String, Object> collectTarget, LoadTestRunContext runContext) throws Exception {
        Objects.requireNonNull(collectTarget, "collectTarget");
        Map<String, Object> merged = new LinkedHashMap<>(collectTarget);
        mergeCredentialsFile(merged);

        String appKey = resolveSecret(merged, "appKey", "LOADTEST_MERCHANT_APP_KEY");
        String appSecret = resolveSecret(merged, "appSecret", "LOADTEST_MERCHANT_APP_SECRET");
        long merchantId = longValue(merged, "merchantId", envLong("LOADTEST_MERCHANT_ID", 0L));
        long amount = longValue(merged, "amount", 100L);
        String channel = stringValue(merged, "channel", "MOCK");
        String productType = stringValue(merged, "productType", "QUICK_COLLECTION");
        String orderPrefix = stringValue(merged, "orderNoPrefix", "LT-C-");
        String orderNo = orderPrefix + runContext.iteration() + "-" + runContext.threadIndex();

        if (appKey == null || appKey.isBlank()) {
            throw new IllegalArgumentException("merchantEncryptedCollect.appKey or LOADTEST_MERCHANT_APP_KEY required");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "merchantEncryptedCollect.appSecret or LOADTEST_MERCHANT_APP_SECRET required");
        }
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantEncryptedCollect.merchantId or LOADTEST_MERCHANT_ID required");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", merchantId);
        payload.put("order_no", orderNo);
        payload.put("amount", amount);
        payload.put("channel", channel);
        payload.put("product_type", productType);
        String payloadPlain = JSON.writeValueAsString(payload);
        String payloadCipher = MerchantOpenApiPayloadCipher.encryptPayloadUtf8(appSecret, payloadPlain);

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> signParams = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                appKey, timestamp, nonce, MerchantOpenApiSignatures.SIGN_METHOD_HMAC_SHA256));
        signParams.put("payload", payloadCipher);
        String sign = MerchantOpenApiSignatures.signHmacSha256Hex(
                appSecret, MerchantOpenApiSignatures.buildCanonical(signParams));

        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("appKey", appKey);
        outer.put("timestamp", timestamp);
        outer.put("nonce", nonce);
        outer.put("signMethod", MerchantOpenApiSignatures.SIGN_METHOD_HMAC_SHA256);
        outer.put("sign", sign);
        outer.put("payload", payloadCipher);
        return JSON.writeValueAsString(outer);
    }

    /**
     * 可选 {@code credentialsFile}：JSON 含 {@code appKey}、{@code appSecret}、{@code merchantId}（由 seed 脚本生成）。
     */
    @SuppressWarnings("unchecked")
    private static void mergeCredentialsFile(Map<String, Object> target) throws Exception {
        Object pathObj = target.get("credentialsFile");
        String pathRaw = pathObj == null || String.valueOf(pathObj).isBlank()
                ? System.getenv("LOADTEST_MERCHANT_CREDENTIALS_FILE")
                : String.valueOf(pathObj);
        if (pathRaw == null || pathRaw.isBlank()) {
            return;
        }
        Path path = Path.of(pathRaw);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("credentialsFile not found: " + path);
        }
        Map<String, Object> file = JSON.readValue(Files.readString(path), Map.class);
        file.forEach((k, v) -> {
            if (!target.containsKey(k) || target.get(k) == null) {
                target.put(k, v);
            }
        });
    }

    private static String resolveSecret(Map<String, Object> target, String yamlKey, String envKey) {
        Object yaml = target.get(yamlKey);
        if (yaml != null && !String.valueOf(yaml).isBlank()) {
            return String.valueOf(yaml);
        }
        String env = System.getenv(envKey);
        return env != null && !env.isBlank() ? env : null;
    }

    private static String stringValue(Map<String, Object> target, String key, String defaultValue) {
        Object v = target.get(key);
        return v == null ? defaultValue : String.valueOf(v);
    }

    private static long longValue(Map<String, Object> target, String key, long defaultValue) {
        Object v = target.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v != null) {
            try {
                return Long.parseLong(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static long envLong(String envKey, long defaultValue) {
        String raw = System.getenv(envKey);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
