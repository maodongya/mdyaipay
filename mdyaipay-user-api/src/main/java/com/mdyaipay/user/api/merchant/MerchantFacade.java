package com.mdyaipay.user.api.merchant;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 商户 Dubbo 门面：平台审核类接口无商户签名；商户侧开放能力必须携带 {@link MerchantSignEnvelope}。
 * <p>网关或支付模块仅依赖本接口 jar，勿依赖 {@code mdyaipay-user} 实现模块。</p>
 */
public interface MerchantFacade {

    /** 平台：创建商户草稿并绑定 OWNER（不走商户 API 签名）。 */
    ApiResponse<CreateMerchantResult> createMerchant(CreateMerchantCommand command);

    /**
     * 平台：签发开放 API 密钥。
     * <p>{@code appSecret} 仅在本响应中明文返回一次，之后只以 AES 密文存 {@code merchant_api_credential}。</p>
     */
    ApiResponse<IssueApiCredentialResult> issueApiCredential(IssueApiCredentialCommand command);

    /** 商户签名：提交审核，状态 DRAFT/REJECTED → PENDING；操作人须为 OWNER/OP。 */
    ApiResponse<Void> submitMerchantAudit(SubmitMerchantAuditRequest request);

    /** 平台：审核通过并自动 enable；写审核记录与 Outbox（AUDIT_APPROVED）。 */
    ApiResponse<Void> approveMerchant(ApproveMerchantCommand command);

    /** 平台：审核驳回；写审核记录与 Outbox（AUDIT_REJECTED）。 */
    ApiResponse<Void> rejectMerchant(RejectMerchantCommand command);

    /** 商户签名：创建店铺；商户须 ENABLED。 */
    ApiResponse<CreateShopResult> createShop(CreateShopRequest request);

    /** 商户签名：更新店铺营业状态 OPEN/CLOSED。 */
    ApiResponse<Void> updateShopOperatingStatus(UpdateShopOperatingStatusRequest request);

    /** 商户签名：添加 OP/FIN 成员；不可添加 OWNER；同 user 幂等跳过。 */
    ApiResponse<Void> addMember(AddMerchantMemberRequest request);

    /** 内网：校验用户是否具备商户角色（payment/gateway 调用，无商户签名）。 */
    ApiResponse<AssertMemberRoleResult> assertMemberRole(AssertMemberRoleQuery query);
}
