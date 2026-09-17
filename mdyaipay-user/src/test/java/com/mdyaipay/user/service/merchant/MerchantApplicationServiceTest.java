package com.mdyaipay.user.service.merchant;

import com.mdyaipay.user.api.merchant.ApproveMerchantCommand;
import com.mdyaipay.user.api.merchant.CreateMerchantCommand;
import com.mdyaipay.user.api.merchant.CreateShopRequest;
import com.mdyaipay.user.api.merchant.IssueApiCredentialCommand;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.api.merchant.SubmitMerchantAuditRequest;
import com.mdyaipay.user.domain.merchant.MerchantStatus;
import com.mdyaipay.user.testsupport.merchant.MapMerchantApiCredentialRepository;
import com.mdyaipay.user.testsupport.merchant.MapMerchantRepository;
import com.mdyaipay.user.testsupport.merchant.MerchantTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

class MerchantApplicationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long TS = FIXED_NOW.toEpochMilli();

    @Test
    void shouldSubmitAuditWhenSignatureValid() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        MerchantApplicationService service = MerchantTestSupport.applicationService(
                merchantRepository, credentialRepository);

        long ownerUserId = 9001L;
        long merchantId = service.createMerchant(new CreateMerchantCommand("shop-co", ownerUserId)).getMerchantId();
        var cred = service.issueApiCredential(new IssueApiCredentialCommand(merchantId, "platform"));

        Map<String, String> business = Map.of(
                "merchant_id", Long.toString(merchantId),
                "operator_user_id", Long.toString(ownerUserId));
        MerchantSignEnvelope signature = MerchantTestSupport.sign(
                cred.getAppKey(), cred.getAppSecret(), TS, "nonce-audit", business);

        service.submitMerchantAudit(new SubmitMerchantAuditRequest(signature, merchantId, ownerUserId));

        Assertions.assertEquals(
                MerchantStatus.PENDING,
                merchantRepository.findById(merchantId).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectSubmitAuditWhenSignatureInvalid() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        MerchantApplicationService service = MerchantTestSupport.applicationService(
                merchantRepository, credentialRepository);

        long merchantId = service.createMerchant(new CreateMerchantCommand("shop-co", 9001L)).getMerchantId();
        service.issueApiCredential(new IssueApiCredentialCommand(merchantId, "platform"));

        MerchantSignEnvelope badSign = new MerchantSignEnvelope(
                "missing", TS, "n1", MerchantSignEnvelope.SIGN_METHOD_HMAC_SHA256, "00");

        Assertions.assertThrows(
                MerchantBusinessException.class,
                () -> service.submitMerchantAudit(new SubmitMerchantAuditRequest(badSign, merchantId, 9001L)));
    }

    @Test
    void shouldCreateShopWhenEnabledAndSigned() {
        MapMerchantRepository merchantRepository = new MapMerchantRepository();
        MapMerchantApiCredentialRepository credentialRepository = new MapMerchantApiCredentialRepository();
        MerchantApplicationService service = MerchantTestSupport.applicationService(
                merchantRepository, credentialRepository);

        long ownerUserId = 9001L;
        long merchantId = service.createMerchant(new CreateMerchantCommand("shop-co", ownerUserId)).getMerchantId();
        var cred = service.issueApiCredential(new IssueApiCredentialCommand(merchantId, "platform"));
        Map<String, String> auditParams = Map.of(
                "merchant_id", Long.toString(merchantId),
                "operator_user_id", Long.toString(ownerUserId));
        service.submitMerchantAudit(new SubmitMerchantAuditRequest(
                MerchantTestSupport.sign(cred.getAppKey(), cred.getAppSecret(), TS, "nonce-audit2", auditParams),
                merchantId,
                ownerUserId));
        service.approveMerchant(new ApproveMerchantCommand(merchantId, "auditor"));

        Map<String, String> business = Map.of(
                "merchant_id", Long.toString(merchantId),
                "shop_name", "旗舰店",
                "category", "retail");
        MerchantSignEnvelope signature = MerchantTestSupport.sign(
                cred.getAppKey(), cred.getAppSecret(), TS, "nonce-shop", business);

        long shopId = service.createShop(new CreateShopRequest(signature, merchantId, "旗舰店", "retail")).getShopId();
        Assertions.assertTrue(shopId > 0);
    }
}
