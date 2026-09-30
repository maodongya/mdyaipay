package com.mdyaipay.finance.api.dubbo;

import com.mdyaipay.finance.api.reconcile.ReconcileCommand;
import com.mdyaipay.finance.api.reconcile.ReconciliationBatchView;
import com.mdyaipay.finance.api.reconcile.ReconciliationDifferenceView;
import com.mdyaipay.finance.api.reconcile.ReconciliationFacade;
import com.mdyaipay.finance.domain.reconcile.BillSource;
import com.mdyaipay.finance.domain.reconcile.DifferenceType;
import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;
import com.mdyaipay.finance.domain.reconcile.ReconciliationDifference;
import com.mdyaipay.finance.service.reconcile.ReconciliationApplicationService;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * {@link ReconciliationFacade} Dubbo 实现。领域规则留在应用服务。
 * <p>不负责读取支付库。</p>
 */
@Component
@DubboService(version = "1.0.0")
public class ReconciliationFacadeImpl implements ReconciliationFacade {

    private final ReconciliationApplicationService reconciliationApplicationService;

    public ReconciliationFacadeImpl(ReconciliationApplicationService reconciliationApplicationService) {
        this.reconciliationApplicationService = reconciliationApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ReconciliationBatchView> reconcile(ReconcileCommand command) {
        return run(() -> toView(reconciliationApplicationService.reconcile(
                command.getChannel(),
                parseDay(command.getBusinessDate()),
                parseSource(command.getBillSource()),
                command.getFilePath() == null ? null : Path.of(command.getFilePath()))));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ReconciliationBatchView> getBatch(String channel, String businessDate) {
        return run(() -> reconciliationApplicationService.find(channel, parseDay(businessDate))
                .map(ReconciliationFacadeImpl::toView)
                .orElseThrow(() -> new IllegalArgumentException("batch not found")));
    }

    private static ReconciliationBatchView toView(ReconciliationBatch batch) {
        List<ReconciliationDifferenceView> differences = batch.differences().stream()
                .map(ReconciliationFacadeImpl::toDifferenceView)
                .toList();
        return new ReconciliationBatchView(
                batch.channel(),
                batch.businessDate().toString(),
                batch.status().name(),
                batch.billSource().name(),
                batch.matchedCount(),
                count(batch, DifferenceType.AMOUNT_MISMATCH),
                count(batch, DifferenceType.LOCAL_ONLY),
                count(batch, DifferenceType.CHANNEL_ONLY),
                differences);
    }

    private static ReconciliationDifferenceView toDifferenceView(ReconciliationDifference difference) {
        return new ReconciliationDifferenceView(
                difference.type().name(),
                difference.channelTradeNo(),
                difference.orderNo(),
                difference.localAmount(),
                difference.channelAmount());
    }

    private static int count(ReconciliationBatch batch, DifferenceType type) {
        return (int) batch.differences().stream().filter(difference -> difference.type() == type).count();
    }

    private static LocalDate parseDay(String businessDate) {
        if (businessDate == null || businessDate.isBlank()) {
            throw new IllegalArgumentException("businessDate must not be blank");
        }
        return LocalDate.parse(businessDate);
    }

    private static BillSource parseSource(String billSource) {
        if (billSource == null || billSource.isBlank()) {
            throw new IllegalArgumentException("billSource must not be blank");
        }
        return BillSource.valueOf(billSource);
    }

    private static <T> ApiResponse<T> run(java.util.concurrent.Callable<T> action) {
        try {
            return ApiResponse.ok(action.call());
        } catch (IllegalArgumentException | IllegalStateException | DateTimeParseException ex) {
            ErrorCode code = ex instanceof IllegalArgumentException && ex.getMessage() != null
                    && ex.getMessage().contains("not found")
                    ? ErrorCode.NOT_FOUND
                    : ErrorCode.INVALID_PARAM;
            return ApiResponse.fail(code.getCode(), ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage());
        }
    }
}
