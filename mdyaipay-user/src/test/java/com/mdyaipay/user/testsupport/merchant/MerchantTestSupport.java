package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.domain.merchant.MerchantRepository;
import com.mdyaipay.user.integration.merchant.sign.MerchantSignatureSupport;
import com.mdyaipay.user.integration.merchant.sign.MerchantSignatureVerifier;
import com.mdyaipay.user.service.merchant.MerchantApplicationService;
import com.mdyaipay.user.service.merchant.MerchantSignGuard;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class MerchantTestSupport {

    private MerchantTestSupport() {
    }

    public static SnowflakeIdGenerator idGenerator() {
        return new SnowflakeIdGenerator(2L, 2L);
    }

    public static MerchantApplicationService applicationService(
            MerchantRepository merchantRepository,
            MerchantApiCredentialRepository credentialRepository) {
        MapMerchantMemberRepository memberRepository = new MapMerchantMemberRepository();
        MapShopRepository shopRepository = new MapShopRepository();
        UserMerchantSignConfig signConfig = UserMerchantSignConfig.forTest();
        MerchantSignatureVerifier verifier = new MerchantSignatureVerifier(
                credentialRepository,
                merchantRepository,
                signConfig,
                Instant.parse("2026-01-01T00:00:00Z"));
        MerchantSignGuard signGuard = new MerchantSignGuard(verifier);
        return new MerchantApplicationService(
                merchantRepository,
                memberRepository,
                new MapMerchantAuditRecordRepository(),
                credentialRepository,
                shopRepository,
                signGuard,
                idGenerator(),
                new NoOpMerchantAuditOutboxRecorder());
    }

    public static MerchantSignEnvelope sign(
            String appKey,
            String secret,
            long timestampMillis,
            String nonce,
            Map<String, String> businessParams) {
        Map<String, String> params = new HashMap<>();
        params.put("app_key", appKey);
        params.put("timestamp", Long.toString(timestampMillis));
        params.put("nonce", nonce);
        params.put("sign_method", MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256);
        params.putAll(businessParams);
        String canonical = MerchantSignatureSupport.buildCanonical(params);
        String sign = MerchantSignatureSupport.signHmacSha256Hex(secret, canonical);
        return new MerchantSignEnvelope(
                appKey,
                timestampMillis,
                nonce,
                MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256,
                sign);
    }
}
