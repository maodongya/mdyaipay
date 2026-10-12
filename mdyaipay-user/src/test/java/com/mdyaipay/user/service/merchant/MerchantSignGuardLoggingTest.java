package com.mdyaipay.user.service.merchant;

import com.mdyaipay.user.api.UserErrorCodes;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.domain.merchant.CredentialStatus;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.integration.merchant.sign.MerchantSignatureVerifier;
import com.mdyaipay.user.testsupport.merchant.MapMerchantApiCredentialRepository;
import com.mdyaipay.user.testsupport.merchant.MapMerchantRepository;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 商户验签失败事件日志。
 */
class MerchantSignGuardLoggingTest {

    /** 验签失败时必须写出稳定动作名。 */
    @Test
    void shouldLogMerchantAuthRejected() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MapMerchantRepository merchants = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentials = new MapMerchantApiCredentialRepository();
        merchants.save(new Merchant(100L, "m1"));
        credentials.save(new MerchantApiCredential(
                1L, 100L, "app_test", "secret-a", CredentialStatus.ACTIVE, now, now));
        MerchantSignGuard guard = new MerchantSignGuard(new MerchantSignatureVerifier(
                credentials, merchants, UserMerchantSignConfig.forTest(), now));
        MerchantSignEnvelope envelope = new MerchantSignEnvelope(
                "app_test", now.toEpochMilli(), "n1", MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256, "deadbeef");

        List<String> messages = LogMessageCapture.capture(MerchantSignGuard.class, () ->
                Assertions.assertThrows(MerchantBusinessException.class, () ->
                        guard.verifyAndResolveMerchantId(envelope, Map.of("merchant_id", "100"))));

        Assertions.assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.MERCHANT_AUTH_REJECTED))));
        Assertions.assertTrue(messages.stream().anyMatch(line ->
                line.contains(String.valueOf(UserErrorCodes.SIGN_INVALID))));
    }
}
