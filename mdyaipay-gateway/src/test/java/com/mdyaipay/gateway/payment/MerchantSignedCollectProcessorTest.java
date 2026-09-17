package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.merchant.MerchantOpenApiPayloadCipher;
import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MerchantSignedCollectProcessorTest {

    private final ObjectMapper json = new ObjectMapper();
    private final MerchantSignedCollectProcessor processor = new MerchantSignedCollectProcessor(json);

    @Test
    void shouldProducePaymentBodyWhenSignValid() throws Exception {
        String secret = "gateway-test-secret";
        long merchantId = 42L;
        long timestamp = System.currentTimeMillis();
        String payloadPlain = """
                {"merchant_id":42,"amount":100,"channel":"MOCK","product_type":"QUICK_COLLECTION"}
                """;
        String payloadCipher = MerchantOpenApiPayloadCipher.encryptPayloadUtf8(secret, payloadPlain.trim());

        Map<String, String> signParams = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                "app-key-1", timestamp, "nonce-1", MerchantOpenApiSignatures.SIGN_METHOD_HMAC_SHA256));
        signParams.put("payload", payloadCipher);
        String sign = MerchantOpenApiSignatures.signHmacSha256Hex(
                secret, MerchantOpenApiSignatures.buildCanonical(signParams));

        String requestJson = json.writeValueAsString(Map.of(
                "appKey", "app-key-1",
                "timestamp", timestamp,
                "nonce", "nonce-1",
                "signMethod", MerchantOpenApiSignatures.SIGN_METHOD_HMAC_SHA256,
                "sign", sign,
                "payload", payloadCipher));

        var credential = new ResolveOpenApiCredentialResult(merchantId, secret, 300);
        byte[] paymentBody = processor.toPaymentCollectBody(requestJson, credential);
        var node = json.readTree(paymentBody);
        Assertions.assertEquals(42L, node.get("merchantId").asLong());
        Assertions.assertEquals(100L, node.get("amount").asLong());
        Assertions.assertEquals("MOCK", node.get("channel").asText());
    }
}
