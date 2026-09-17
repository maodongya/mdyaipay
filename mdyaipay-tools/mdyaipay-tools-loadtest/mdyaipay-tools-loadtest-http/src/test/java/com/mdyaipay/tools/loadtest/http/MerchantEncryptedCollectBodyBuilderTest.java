package com.mdyaipay.tools.loadtest.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.merchant.MerchantOpenApiPayloadCipher;
import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MerchantEncryptedCollectBodyBuilderTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void shouldBuildVerifiableEncryptedCollectBody() throws Exception {
        String secret = "loadtest-secret";
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("appKey", "loadtest-key");
        cfg.put("appSecret", secret);
        cfg.put("merchantId", 9L);
        cfg.put("amount", 100L);
        cfg.put("channel", "MOCK");

        String body = MerchantEncryptedCollectBodyBuilder.build(cfg, new LoadTestRunContext(3, 2));
        JsonNode root = json.readTree(body);
        Assertions.assertEquals("loadtest-key", root.get("appKey").asText());
        String payloadCipher = root.get("payload").asText();
        String plain = MerchantOpenApiPayloadCipher.decryptPayloadUtf8(secret, payloadCipher);
        JsonNode payload = json.readTree(plain);
        Assertions.assertEquals(9L, payload.get("merchant_id").asLong());
        Assertions.assertTrue(payload.get("order_no").asText().startsWith("LT-C-"));

        Map<String, String> signParams = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                root.get("appKey").asText(),
                root.get("timestamp").asLong(),
                root.get("nonce").asText(),
                root.get("signMethod").asText()));
        signParams.put("payload", payloadCipher);
        String expected = MerchantOpenApiSignatures.signHmacSha256Hex(
                secret, MerchantOpenApiSignatures.buildCanonical(signParams));
        Assertions.assertEquals(expected, root.get("sign").asText());
    }
}
