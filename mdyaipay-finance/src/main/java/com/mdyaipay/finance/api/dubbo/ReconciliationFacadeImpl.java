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
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOG = LogManager.getLogger(ReconciliationFacadeImpl.class);

    private final ReconciliationApplicationService reconciliationApplicationService;

    /**
     * @param reconciliationApplicationService 对账编排，不能为空
     */
    public ReconciliationFacadeImpl(ReconciliationApplicationService reconciliationApplicationService) {
        this.reconciliationApplicationService = reconciliationApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ReconciliationBatchView> reconcile(ReconcileCommand command) {
        ApiResponse<ReconciliationBatchView> response = run(() -> toView(reconciliationApplicationService.reconcile(
                command.getChannel(),
                parseDay(command.getBusinessDate()),
                parseSource(command.getBillSource()),
                command.getFilePath() == null ? null : Path.of(command.getFilePath()))));
        if (response.getData() != null
                && response.getData().getDifferences() != null
                && !response.getData().getDifferences().isEmpty()) {
            LOG.warn(
                    "{} difference_count={}",
                    AlertEvents.failure(AlertEvents.RECONCILIATION_DIFFERENCE_FOUND),
                    response.getData().getDifferences().size());
        }
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ReconciliationBatchView> getBatch(String channel, String businessDate) {
        return run(() -> reconciliationApplicationService.find(channel, parseDay(businessDate))
                .map(ReconciliationFacadeImpl::toView)
                .orElseThrow(() -> new IllegalArgumentException("batch not found")));
    }

    /** 批次转视图；不含仓储读写。 */
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

    /** 单条差异转视图。 */
    private static ReconciliationDifferenceView toDifferenceView(ReconciliationDifference difference) {
        return new ReconciliationDifferenceView(
                difference.type().name(),
                difference.channelTradeNo(),
                difference.orderNo(),
                difference.localAmount(),
                difference.channelAmount());
    }

    /** 统计某一类差异条数。 */
    private static int count(ReconciliationBatch batch, DifferenceType type) {
        return (int) batch.differences().stream().filter(difference -> difference.type() == type).count();
    }

    /** 解析业务日；空串拒绝。 */
    private static LocalDate parseDay(String businessDate) {
        if (businessDate == null || businessDate.isBlank()) {
            throw new IllegalArgumentException("businessDate must not be blank");
        }
        return LocalDate.parse(businessDate);
    }

    /** 解析账单来源枚举；空串拒绝。 */
    private static BillSource parseSource(String billSource) {
        if (billSource == null || billSource.isBlank()) {
            throw new IllegalArgumentException("billSource must not be blank");
        }
        return BillSource.valueOf(billSource);
    }

    /**
     * 把参数错误与已处理批次映射为业务码；其余失败写 {@code reconciliation_failed}。
     * <p>无额外副作用；不改批次状态。
     */
    private <T> ApiResponse<T> run(java.util.concurrent.Callable<T> action) {
        try {
            return ApiResponse.ok(action.call());
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            ErrorCode code = ex instanceof IllegalArgumentException && ex.getMessage() != null
                    && ex.getMessage().contains("not found")
                    ? ErrorCode.NOT_FOUND
                    : ErrorCode.INVALID_PARAM;
            return ApiResponse.fail(code.getCode(), ex.getMessage());
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException && "batch already processed".equals(ex.getMessage())) {
                return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), ex.getMessage());
            }
            LOG.error(AlertEvents.failure(AlertEvents.RECONCILIATION_FAILED), ex);
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage());
        }
    }
}
