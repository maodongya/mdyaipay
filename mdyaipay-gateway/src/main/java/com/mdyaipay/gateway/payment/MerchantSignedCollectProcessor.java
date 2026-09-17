package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.merchant.MerchantOpenApiPayloadCipher;
import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 商户加密收单：查凭证 → 验签 → 解密 payload → 转为 payment 收单 JSON。
 */
public final class MerchantSignedCollectProcessor {

    private final ObjectMapper json;

    public MerchantSignedCollectProcessor(ObjectMapper json) {
        this.json = Objects.requireNonNull(json, "json");
    }

    public byte[] toPaymentCollectBody(String encryptedRequestJson, ResolveOpenApiCredentialResult credential)
            throws MerchantSignedCollectException {
        try {
            JsonNode root = json.readTree(encryptedRequestJson);
            String appKey = requiredText(root, "appKey", "app_key");
            long timestamp = requiredLong(root, "timestamp", "timestampMillis", "timestamp_millis");
            String nonce = requiredText(root, "nonce");
            String signMethod = requiredText(root, "signMethod", "sign_method");
            String sign = requiredText(root, "sign");
            String payloadCipher = requiredText(root, "payload");

            /* 功能块：时间窗 — 与 user 模块 maxSkew 一致 */
            long skewMillis = credential.getMaxSkewSeconds() * 1000L;
            long delta = Math.abs(Instant.now().toEpochMilli() - timestamp);
            if (delta > skewMillis) {
                throw MerchantSignedCollectException.signExpired();
            }

            if (!MerchantOpenApiSignatures.SIGN_METHOD_HMAC_SHA256.equals(signMethod)) {
                throw MerchantSignedCollectException.signInvalid();
            }

            /* 功能块：外层验签 — payload 密文参与 canonical，防篡改 */
            Map<String, String> signParams = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                    appKey, timestamp, nonce, signMethod));
            signParams.put("payload", payloadCipher);
            String canonical = MerchantOpenApiSignatures.buildCanonical(signParams);
            String expected = MerchantOpenApiSignatures.signHmacSha256Hex(
                    credential.getAppSecretPlain(), canonical);
            if (!MerchantOpenApiSignatures.constantTimeEquals(expected, sign)) {
                throw MerchantSignedCollectException.signInvalid();
            }

            /* 功能块：解密内层 payload — 业务字段在明文 JSON 中校验 */
            String plainPayload = MerchantOpenApiPayloadCipher.decryptPayloadUtf8(
                    credential.getAppSecretPlain(), payloadCipher);
            JsonNode payload = json.readTree(plainPayload);
            long merchantId = longField(payload, "merchantId", "merchant_id");
            String orderNo = textField(payload, "orderNo", "order_no");
            long amount = longField(payload, "amount");
            String channel = requiredText(payload, "channel");
            String productType = textField(payload, "productType", "product_type");

            if (merchantId != credential.getMerchantId()) {
                throw MerchantSignedCollectException.merchantMismatch();
            }
            if (amount <= 0 || channel.isBlank()) {
                throw MerchantSignedCollectException.badPayload();
            }

            GatewayCollectPaymentRequest paymentBody = new GatewayCollectPaymentRequest(
                    merchantId, orderNo, amount, channel, productType);
            return json.writeValueAsBytes(paymentBody);
        } catch (MerchantSignedCollectException ex) {
            throw ex;
        } catch (Exception ex) {
            throw MerchantSignedCollectException.badRequest(ex.getMessage());
        }
    }

    private static String requiredText(JsonNode root, String... names) throws MerchantSignedCollectException {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        throw MerchantSignedCollectException.badRequest("missing field: " + names[0]);
    }

    private static long longField(JsonNode root, String... names) throws MerchantSignedCollectException {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.canConvertToLong()) {
                return node.asLong();
            }
        }
        throw MerchantSignedCollectException.badPayload();
    }

    private static String textField(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private static String requiredText(JsonNode root, String name) throws MerchantSignedCollectException {
        JsonNode node = root.get(name);
        if (node != null && !node.asText().isBlank()) {
            return node.asText();
        }
        throw MerchantSignedCollectException.badPayload();
    }

    private static long requiredLong(JsonNode root, String... names) throws MerchantSignedCollectException {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.canConvertToLong()) {
                long value = node.asLong();
                if (value > 0) {
                    return value;
                }
            }
        }
        throw MerchantSignedCollectException.badRequest("missing field: " + names[0]);
    }

    /** payment {@code CollectPaymentRequest} 对齐字段。 */
    public record GatewayCollectPaymentRequest(
            long merchantId,
            String orderNo,
            long amount,
            String channel,
            String productType) {
    }
}
