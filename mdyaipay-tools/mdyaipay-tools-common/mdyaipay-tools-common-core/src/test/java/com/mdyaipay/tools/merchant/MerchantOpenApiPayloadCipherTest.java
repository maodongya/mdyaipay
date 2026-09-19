package com.mdyaipay.tools.merchant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MerchantOpenApiPayloadCipherTest {

    @Test
    void shouldRoundTripPayload() {
        String secret = "test-app-secret-001";
        String plain = "{\"merchant_id\":1,\"amount\":100,\"channel\":\"MOCK\"}";
        String cipher = MerchantOpenApiPayloadCipher.encryptPayloadUtf8(secret, plain);
        String decrypted = MerchantOpenApiPayloadCipher.decryptPayloadUtf8(secret, cipher);
        Assertions.assertEquals(plain, decrypted);
    }
}
