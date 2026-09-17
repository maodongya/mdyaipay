package com.mdyaipay.tools.merchant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 商户开放 API 签名：参与字段按 key 字典序 {@code k=v&…}（不含 {@code sign}），HMAC-SHA256 后 hex 小写。
 */
public final class MerchantOpenApiSignatures {

    public static final String SIGN_METHOD_HMAC_SHA256 = "HMAC_SHA256";

    private MerchantOpenApiSignatures() {
    }

    public static String buildCanonical(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("sign".equals(entry.getKey())) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    public static String signHmacSha256Hex(String secret, String canonical) {
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(canonical, "canonical must not be null");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return toHexLower(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC sign failed", ex);
        }
    }

    public static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> envelopeParams(
            String appKey, long timestampMillis, String nonce, String signMethod) {
        Map<String, String> params = new TreeMap<>();
        params.put("app_key", appKey);
        params.put("timestamp", Long.toString(timestampMillis));
        params.put("nonce", nonce);
        params.put("sign_method", signMethod);
        return params;
    }

    private static String toHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
