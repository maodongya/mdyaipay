package com.mdyaipay.user.integration.crypto;

import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AesSecretCipherTest {

    @Test
    void shouldRoundTripSecret() {
        AesSecretCipher cipher = new AesSecretCipher(UserMerchantSignConfig.forTest().getAesKeyBase64());
        String plain = "merchant-api-secret-value";
        String encrypted = cipher.encrypt(plain);
        Assertions.assertNotEquals(plain, encrypted);
        Assertions.assertEquals(plain, cipher.decrypt(encrypted));
    }
}
