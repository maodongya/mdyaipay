package com.mdyaipay.user.integration.merchant.sign;

import com.mdyaipay.user.api.UserErrorCodes;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.domain.merchant.CredentialStatus;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantStatus;
import com.mdyaipay.user.testsupport.merchant.MapMerchantApiCredentialRepository;
import com.mdyaipay.user.testsupport.merchant.MapMerchantRepository;
import com.mdyaipay.user.testsupport.merchant.MerchantTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

class MerchantSignatureVerifierTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long TS = FIXED_NOW.toEpochMilli();

    @Test
    void shouldVerifyValidSignature() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        merchantRepository.save(new Merchant(100L, "m1"));

        String secret = "test-secret";
        credentialRepository.save(new MerchantApiCredential(
                1L,
                100L,
                "app_test",
                secret,
                CredentialStatus.ACTIVE,
                FIXED_NOW,
                FIXED_NOW));

        MerchantSignatureVerifier verifier = new MerchantSignatureVerifier(
                credentialRepository,
                merchantRepository,
                UserMerchantSignConfig.forTest(),
                FIXED_NOW);

        Map<String, String> business = Map.of("merchant_id", "100");
        MerchantSignEnvelope envelope = MerchantTestSupport.sign(
                "app_test", secret, TS, "nonce-1", business);

        SignVerifyResult result = verifier.verify(envelope, business);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(100L, result.getMerchantId());
    }

    @Test
    void shouldFailWhenSignatureWrong() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        merchantRepository.save(new Merchant(100L, "m1"));
        credentialRepository.save(new MerchantApiCredential(
                1L, 100L, "app_test", "secret-a", CredentialStatus.ACTIVE, FIXED_NOW, FIXED_NOW));

        MerchantSignatureVerifier verifier = new MerchantSignatureVerifier(
                credentialRepository,
                merchantRepository,
                UserMerchantSignConfig.forTest(),
                FIXED_NOW);

        MerchantSignEnvelope envelope = new MerchantSignEnvelope(
                "app_test", TS, "n1", MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256, "deadbeef");

        SignVerifyResult result = verifier.verify(envelope, Map.of("merchant_id", "100"));
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(UserErrorCodes.SIGN_INVALID, result.getErrorCode());
    }

    @Test
    void shouldFailWhenTimestampExpired() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        merchantRepository.save(new Merchant(100L, "m1"));
        String secret = "secret-a";
        credentialRepository.save(new MerchantApiCredential(
                1L, 100L, "app_test", secret, CredentialStatus.ACTIVE, FIXED_NOW, FIXED_NOW));

        MerchantSignatureVerifier verifier = new MerchantSignatureVerifier(
                credentialRepository,
                merchantRepository,
                UserMerchantSignConfig.forTest(),
                FIXED_NOW);

        long oldTs = TS - 600_000L;
        MerchantSignEnvelope envelope = MerchantTestSupport.sign(
                "app_test", secret, oldTs, "n1", Map.of("merchant_id", "100"));

        SignVerifyResult result = verifier.verify(envelope, Map.of("merchant_id", "100"));
        Assertions.assertEquals(UserErrorCodes.SIGN_EXPIRED, result.getErrorCode());
    }
}
