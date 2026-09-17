package com.mdyaipay.user.integration.merchant.sign;

import com.mdyaipay.user.api.UserErrorCodes;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.domain.merchant.MerchantRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 校验商户开放 API 签名，并确认 appKey 对应商户处于可调用状态。
 * <p>顺序：sign_method → 时间窗 → 密钥状态 → 商户状态 → 重算 HMAC → merchant_id 与 body 一致。</p>
 */
public final class MerchantSignatureVerifier {

    private final MerchantApiCredentialRepository credentialRepository;
    private final MerchantRepository merchantRepository;
    private final UserMerchantSignConfig signConfig;
    private final Instant now;

    public MerchantSignatureVerifier(
            MerchantApiCredentialRepository credentialRepository,
            MerchantRepository merchantRepository,
            UserMerchantSignConfig signConfig) {
        this(credentialRepository, merchantRepository, signConfig, Instant.now());
    }

    public MerchantSignatureVerifier(
            MerchantApiCredentialRepository credentialRepository,
            MerchantRepository merchantRepository,
            UserMerchantSignConfig signConfig,
            Instant now) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "credentialRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.signConfig = Objects.requireNonNull(signConfig, "signConfig");
        this.now = Objects.requireNonNull(now, "now");
    }

    /**
     * 校验商户请求签名。
     *
     * @param envelope       公共签名字段
     * @param businessParams 参与 canonical 的业务键值（可为 null）
     */
    public SignVerifyResult verify(MerchantSignEnvelope envelope, Map<String, String> businessParams) {
        /* 功能块：算法与时间窗 — 过期请求直接拒绝，防重放窗口外攻击 */
        if (!MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256.equals(envelope.getSignMethod())) {
            return SignVerifyResult.fail(UserErrorCodes.SIGN_INVALID);
        }
        long skewMillis = signConfig.getMaxSkewSeconds() * 1000L;
        long delta = Math.abs(now.toEpochMilli() - envelope.getTimestampMillis());
        if (delta > skewMillis) {
            return SignVerifyResult.fail(UserErrorCodes.SIGN_EXPIRED);
        }

        /* 功能块：密钥与商户状态 — DISABLED 商户不可调开放 API */
        MerchantApiCredential credential = credentialRepository.findByAppKey(envelope.getAppKey()).orElse(null);
        if (credential == null || !credential.isActive()) {
            return SignVerifyResult.fail(UserErrorCodes.CREDENTIAL_DISABLED);
        }

        Merchant merchant = merchantRepository.findById(credential.getMerchantId()).orElse(null);
        if (merchant == null || !merchant.allowsMerchantApi()) {
            return SignVerifyResult.fail(UserErrorCodes.MERCHANT_STATE_INVALID);
        }

        /* 功能块：重算 HMAC — 与商户端 MerchantSignatureSupport 规则一致 */
        Map<String, String> params = new HashMap<>(MerchantSignatureSupport.envelopeToParams(envelope));
        if (businessParams != null) {
            params.putAll(businessParams);
        }
        String canonical = MerchantSignatureSupport.buildCanonical(params);
        String expected = MerchantSignatureSupport.signHmacSha256Hex(credential.getSecretPlain(), canonical);
        if (!MerchantSignatureSupport.constantTimeEquals(expected, envelope.getSign())) {
            return SignVerifyResult.fail(UserErrorCodes.SIGN_INVALID);
        }

        if (businessParams != null && businessParams.containsKey("merchant_id")) {
            long bodyMerchantId = Long.parseLong(businessParams.get("merchant_id"));
            if (bodyMerchantId != credential.getMerchantId()) {
                return SignVerifyResult.fail(UserErrorCodes.SIGN_INVALID);
            }
        }

        return SignVerifyResult.ok(credential.getMerchantId());
    }
}
