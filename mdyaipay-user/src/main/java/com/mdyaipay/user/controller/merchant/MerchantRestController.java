package com.mdyaipay.user.controller.merchant;

import com.mdyaipay.tools.model.ApiResponse;
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
import com.mdyaipay.user.api.merchant.MerchantFacade;
import com.mdyaipay.user.api.merchant.RejectMerchantCommand;
import com.mdyaipay.user.api.merchant.SubmitMerchantAuditRequest;
import com.mdyaipay.user.api.merchant.UpdateShopOperatingStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户 HTTP 接入：与 {@link MerchantFacade} 能力一致，供 {@code mdyaipay-gateway} 反向代理。
 * <p>响应统一为 {@link ApiResponse}；业务错误码在 body {@code code} 字段，与 Dubbo 一致。</p>
 */
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantRestController {

    private final MerchantFacade merchantFacade;

    public MerchantRestController(MerchantFacade merchantFacade) {
        this.merchantFacade = merchantFacade;
    }

    /** 见 {@link MerchantFacade#createMerchant}。 */
    @PostMapping
    public ApiResponse<CreateMerchantResult> createMerchant(@RequestBody CreateMerchantCommand command) {
        return merchantFacade.createMerchant(command);
    }

    /** 见 {@link MerchantFacade#issueApiCredential}。 */
    @PostMapping("/credentials/issue")
    public ApiResponse<IssueApiCredentialResult> issueApiCredential(@RequestBody IssueApiCredentialCommand command) {
        return merchantFacade.issueApiCredential(command);
    }

    /** 见 {@link MerchantFacade#submitMerchantAudit}。 */
    @PostMapping("/audit/submit")
    public ApiResponse<Void> submitMerchantAudit(@RequestBody SubmitMerchantAuditRequest request) {
        return merchantFacade.submitMerchantAudit(request);
    }

    /** 见 {@link MerchantFacade#approveMerchant}。 */
    @PostMapping("/audit/approve")
    public ApiResponse<Void> approveMerchant(@RequestBody ApproveMerchantCommand command) {
        return merchantFacade.approveMerchant(command);
    }

    /** 见 {@link MerchantFacade#rejectMerchant}。 */
    @PostMapping("/audit/reject")
    public ApiResponse<Void> rejectMerchant(@RequestBody RejectMerchantCommand command) {
        return merchantFacade.rejectMerchant(command);
    }

    /** 见 {@link MerchantFacade#createShop}。 */
    @PostMapping("/shops")
    public ApiResponse<CreateShopResult> createShop(@RequestBody CreateShopRequest request) {
        return merchantFacade.createShop(request);
    }

    /** 见 {@link MerchantFacade#updateShopOperatingStatus}。 */
    @PostMapping("/shops/operating-status")
    public ApiResponse<Void> updateShopOperatingStatus(@RequestBody UpdateShopOperatingStatusRequest request) {
        return merchantFacade.updateShopOperatingStatus(request);
    }

    /** 见 {@link MerchantFacade#addMember}。 */
    @PostMapping("/members")
    public ApiResponse<Void> addMember(@RequestBody AddMerchantMemberRequest request) {
        return merchantFacade.addMember(request);
    }

    /** 见 {@link MerchantFacade#assertMemberRole}。 */
    @GetMapping("/members/assert")
    public ApiResponse<AssertMemberRoleResult> assertMemberRole(
            @RequestParam("merchantId") long merchantId,
            @RequestParam("userId") long userId,
            @RequestParam("requiredRole") String requiredRole) {
        return merchantFacade.assertMemberRole(new AssertMemberRoleQuery(merchantId, userId, requiredRole));
    }
}
