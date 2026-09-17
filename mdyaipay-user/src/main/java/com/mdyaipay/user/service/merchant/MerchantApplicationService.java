package com.mdyaipay.user.service.merchant;

import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.user.api.UserErrorCodes;
import com.mdyaipay.user.api.merchant.AddMerchantMemberRequest;
import com.mdyaipay.user.api.merchant.ApproveMerchantCommand;
import com.mdyaipay.user.api.merchant.AssertMemberRoleQuery;
import com.mdyaipay.user.api.merchant.AssertMemberRoleResult;
import com.mdyaipay.user.api.merchant.CreateMerchantCommand;
import com.mdyaipay.user.api.merchant.CreateMerchantResult;
import com.mdyaipay.user.api.merchant.CreateShopRequest;
import com.mdyaipay.user.api.merchant.CreateShopResult;
import com.mdyaipay.user.api.merchant.IssueApiCredentialCommand;
import com.mdyaipay.user.api.merchant.IssueApiCredentialResult;
import com.mdyaipay.user.api.merchant.RejectMerchantCommand;
import com.mdyaipay.user.api.merchant.SubmitMerchantAuditRequest;
import com.mdyaipay.user.api.merchant.UpdateShopOperatingStatusRequest;
import com.mdyaipay.user.domain.merchant.CredentialStatus;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.domain.merchant.MerchantAuditRecord;
import com.mdyaipay.user.domain.merchant.MerchantAuditRecordRepository;
import com.mdyaipay.user.domain.merchant.MerchantMember;
import com.mdyaipay.user.domain.merchant.MerchantMemberRepository;
import com.mdyaipay.user.domain.merchant.MerchantMemberRole;
import com.mdyaipay.user.domain.merchant.MerchantRepository;
import com.mdyaipay.user.domain.merchant.MerchantStatus;
import com.mdyaipay.user.domain.merchant.Shop;
import com.mdyaipay.user.domain.merchant.ShopOperatingStatus;
import com.mdyaipay.user.domain.merchant.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 商户应用服务：平台路径与商户签名路径分离编排。
 * <p>代扣/支付等模块请通过 Dubbo {@link com.mdyaipay.user.api.merchant.MerchantFacade} 调用，勿绕过验签。</p>
 */
@Service
public class MerchantApplicationService {

    private final MerchantRepository merchantRepository;
    private final MerchantMemberRepository memberRepository;
    private final MerchantAuditRecordRepository auditRecordRepository;
    private final MerchantApiCredentialRepository credentialRepository;
    private final ShopRepository shopRepository;
    private final MerchantSignGuard signGuard;
    private final SnowflakeIdGenerator idGenerator;
    private final MerchantAuditOutboxRecorder auditOutboxRecorder;

    public MerchantApplicationService(
            MerchantRepository merchantRepository,
            MerchantMemberRepository memberRepository,
            MerchantAuditRecordRepository auditRecordRepository,
            MerchantApiCredentialRepository credentialRepository,
            ShopRepository shopRepository,
            MerchantSignGuard signGuard,
            SnowflakeIdGenerator idGenerator,
            MerchantAuditOutboxRecorder auditOutboxRecorder) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository);
        this.memberRepository = Objects.requireNonNull(memberRepository);
        this.auditRecordRepository = Objects.requireNonNull(auditRecordRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.shopRepository = Objects.requireNonNull(shopRepository);
        this.signGuard = Objects.requireNonNull(signGuard);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.auditOutboxRecorder = Objects.requireNonNull(auditOutboxRecorder);
    }

    /** 平台创建商户：无验签；幂等由调用方保证 merchantId 唯一（本方法每次新建）。 */
    public CreateMerchantResult createMerchant(CreateMerchantCommand command) {
        long merchantId = idGenerator.nextId();
        Merchant merchant = new Merchant(merchantId, command.getMerchantName());
        merchantRepository.save(merchant);
        MerchantMember owner = new MerchantMember(
                idGenerator.nextId(),
                merchantId,
                command.getOwnerUserId(),
                MerchantMemberRole.OWNER);
        memberRepository.save(owner);
        return new CreateMerchantResult(merchantId);
    }

    /**
     * 平台签发 API 密钥：返回明文 secret 仅一次；持久化由 dao 层 AES 加密。
     * 前置：商户非 DISABLED。
     */
    public IssueApiCredentialResult issueApiCredential(IssueApiCredentialCommand command) {
        Merchant merchant = merchantRepository.findById(command.getMerchantId())
                .orElseThrow(() -> new MerchantBusinessException(
                        UserErrorCodes.MERCHANT_NOT_FOUND, "merchant not found"));
        if (merchant.getStatus() == MerchantStatus.DISABLED) {
            throw new MerchantBusinessException(
                    UserErrorCodes.MERCHANT_STATE_INVALID, "disabled merchant cannot issue credential");
        }
        String appKey = "mk_" + command.getMerchantId() + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String secret = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        MerchantApiCredential credential = new MerchantApiCredential(
                idGenerator.nextId(),
                command.getMerchantId(),
                appKey,
                secret,
                CredentialStatus.ACTIVE,
                Instant.now(),
                Instant.now());
        credentialRepository.save(credential);
        return new IssueApiCredentialResult(appKey, secret);
    }

    /** 商户提交审核：须验签 + 操作人 OP 及以上。 */
    public void submitMerchantAudit(SubmitMerchantAuditRequest request) {
        /* 功能块：验签 — 商户开放 API 统一入口 */
        Map<String, String> params = Map.of(
                "merchant_id", Long.toString(request.getMerchantId()),
                "operator_user_id", Long.toString(request.getOperatorUserId()));
        long signedMerchantId = signGuard.verifyAndResolveMerchantId(request.getSignature(), params);
        if (signedMerchantId != request.getMerchantId()) {
            throw new MerchantBusinessException(UserErrorCodes.SIGN_INVALID, "merchant_id mismatch");
        }
        assertOperatorMember(request.getMerchantId(), request.getOperatorUserId(), MerchantMemberRole.OP);
        Merchant merchant = loadMerchant(request.getMerchantId());
        merchant.submitAudit();
        merchantRepository.save(merchant);
    }

    /** 平台审核通过：approve + enable，写审核流水与 Outbox（同事务）。 */
    @Transactional
    public void approveMerchant(ApproveMerchantCommand command) {
        Merchant merchant = loadMerchant(command.getMerchantId());
        merchant.approve();
        merchant.enable();
        merchantRepository.save(merchant);
        auditRecordRepository.save(new MerchantAuditRecord(
                idGenerator.nextId(),
                command.getMerchantId(),
                command.getAuditor(),
                "APPROVED",
                null,
                Instant.now()));
        /* 功能块：Outbox — 与审核记录同事务，Relay 异步发 MQ */
        auditOutboxRecorder.recordApproved(merchant, command.getAuditor());
    }

    /** 平台审核驳回：写审核流水与 Outbox（同事务）。 */
    @Transactional
    public void rejectMerchant(RejectMerchantCommand command) {
        Merchant merchant = loadMerchant(command.getMerchantId());
        merchant.reject();
        merchantRepository.save(merchant);
        auditRecordRepository.save(new MerchantAuditRecord(
                idGenerator.nextId(),
                command.getMerchantId(),
                command.getAuditor(),
                "REJECTED",
                command.getRemark(),
                Instant.now()));
        auditOutboxRecorder.recordRejected(merchant, command.getAuditor(), command.getRemark());
    }

    /** 商户创建店铺：须验签；前置状态 ENABLED。 */
    public CreateShopResult createShop(CreateShopRequest request) {
        /* 功能块：验签 — 业务字段参与 canonical */
        Map<String, String> params = Map.of(
                "merchant_id", Long.toString(request.getMerchantId()),
                "shop_name", request.getShopName(),
                "category", request.getCategory() == null ? "" : request.getCategory());
        long signedMerchantId = signGuard.verifyAndResolveMerchantId(request.getSignature(), params);
        if (signedMerchantId != request.getMerchantId()) {
            throw new MerchantBusinessException(UserErrorCodes.SIGN_INVALID, "merchant_id mismatch");
        }
        Merchant merchant = loadMerchant(request.getMerchantId());
        if (merchant.getStatus() != MerchantStatus.ENABLED) {
            throw new MerchantBusinessException(UserErrorCodes.MERCHANT_STATE_INVALID, "merchant not enabled");
        }
        Shop shop = new Shop(
                idGenerator.nextId(), request.getMerchantId(), request.getShopName(), request.getCategory());
        shopRepository.save(shop);
        return new CreateShopResult(shop.getShopId());
    }

    /** 更新店铺营业状态：须验签；shop 须归属签名商户。 */
    public void updateShopOperatingStatus(UpdateShopOperatingStatusRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new MerchantBusinessException(UserErrorCodes.MERCHANT_NOT_FOUND, "shop not found"));
        Map<String, String> params = Map.of(
                "shop_id", Long.toString(request.getShopId()),
                "merchant_id", Long.toString(shop.getMerchantId()),
                "operating_status", request.getOperatingStatus());
        long signedMerchantId = signGuard.verifyAndResolveMerchantId(request.getSignature(), params);
        if (signedMerchantId != shop.getMerchantId()) {
            throw new MerchantBusinessException(UserErrorCodes.SIGN_INVALID, "merchant_id mismatch");
        }
        shop.updateOperatingStatus(ShopOperatingStatus.valueOf(request.getOperatingStatus()));
        shopRepository.save(shop);
    }

    /** 添加成员：须验签；禁止 OWNER；已存在则幂等返回。 */
    public void addMember(AddMerchantMemberRequest request) {
        Map<String, String> params = Map.of(
                "merchant_id", Long.toString(request.getMerchantId()),
                "user_id", Long.toString(request.getUserId()),
                "role", request.getRole());
        long signedMerchantId = signGuard.verifyAndResolveMerchantId(request.getSignature(), params);
        if (signedMerchantId != request.getMerchantId()) {
            throw new MerchantBusinessException(UserErrorCodes.SIGN_INVALID, "merchant_id mismatch");
        }
        if (MerchantMemberRole.OWNER.name().equals(request.getRole())) {
            throw new MerchantBusinessException(UserErrorCodes.MEMBER_FORBIDDEN, "cannot add OWNER via API");
        }
        if (memberRepository.findByMerchantIdAndUserId(request.getMerchantId(), request.getUserId()).isPresent()) {
            return;
        }
        MerchantMember member = new MerchantMember(
                idGenerator.nextId(),
                request.getMerchantId(),
                request.getUserId(),
                MerchantMemberRole.valueOf(request.getRole()));
        memberRepository.save(member);
    }

    /** 内网 RBAC 校验：无商户签名，供 payment/gateway 调用。 */
    public AssertMemberRoleResult assertMemberRole(AssertMemberRoleQuery query) {
        MerchantMember member = memberRepository
                .findByMerchantIdAndUserId(query.getMerchantId(), query.getUserId())
                .orElse(null);
        if (member == null) {
            return new AssertMemberRoleResult(false, null);
        }
        MerchantMemberRole required = MerchantMemberRole.valueOf(query.getRequiredRole());
        return new AssertMemberRoleResult(member.getRole().satisfies(required), member.getRole().name());
    }

    private Merchant loadMerchant(long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantBusinessException(
                        UserErrorCodes.MERCHANT_NOT_FOUND, "merchant not found"));
    }

    private void assertOperatorMember(long merchantId, long userId, MerchantMemberRole required) {
        MerchantMember member = memberRepository.findByMerchantIdAndUserId(merchantId, userId).orElse(null);
        if (member == null || !member.getRole().satisfies(required)) {
            throw new MerchantBusinessException(UserErrorCodes.MEMBER_FORBIDDEN, "operator not allowed");
        }
    }
}
