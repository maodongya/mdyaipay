package com.mdyaipay.user.api.dubbo.merchant;

import com.mdyaipay.tools.exception.ErrorCode;
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
import com.mdyaipay.user.service.merchant.MerchantApplicationService;
import com.mdyaipay.user.service.merchant.MerchantBusinessException;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 商户 Dubbo Provider：捕获 {@link MerchantBusinessException} 映射为 {@link ApiResponse} 业务码。
 */
@Component
@DubboService(version = "1.0.0")
public class MerchantFacadeImpl implements MerchantFacade {

    private final MerchantApplicationService merchantApplicationService;

    public MerchantFacadeImpl(MerchantApplicationService merchantApplicationService) {
        this.merchantApplicationService = merchantApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<CreateMerchantResult> createMerchant(CreateMerchantCommand command) {
        return run(() -> merchantApplicationService.createMerchant(command));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<IssueApiCredentialResult> issueApiCredential(IssueApiCredentialCommand command) {
        return run(() -> merchantApplicationService.issueApiCredential(command));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> submitMerchantAudit(SubmitMerchantAuditRequest request) {
        return runVoid(() -> merchantApplicationService.submitMerchantAudit(request));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> approveMerchant(ApproveMerchantCommand command) {
        return runVoid(() -> merchantApplicationService.approveMerchant(command));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> rejectMerchant(RejectMerchantCommand command) {
        return runVoid(() -> merchantApplicationService.rejectMerchant(command));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<CreateShopResult> createShop(CreateShopRequest request) {
        return run(() -> merchantApplicationService.createShop(request));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> updateShopOperatingStatus(UpdateShopOperatingStatusRequest request) {
        return runVoid(() -> merchantApplicationService.updateShopOperatingStatus(request));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> addMember(AddMerchantMemberRequest request) {
        return runVoid(() -> merchantApplicationService.addMember(request));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<AssertMemberRoleResult> assertMemberRole(AssertMemberRoleQuery query) {
        return run(() -> merchantApplicationService.assertMemberRole(query));
    }

    /** 统一异常映射：不向 Consumer 抛 unchecked，便于网关解析 {@code code}。 */
    private static <T> ApiResponse<T> run(SupplierWithException<T> supplier) {
        try {
            return ApiResponse.ok(supplier.get());
        } catch (MerchantBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), ex.getMessage());
        }
    }

    private static ApiResponse<Void> runVoid(RunnableWithException runnable) {
        try {
            runnable.run();
            return ApiResponse.ok(null);
        } catch (MerchantBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get();
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run();
    }
}
