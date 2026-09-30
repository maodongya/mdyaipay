package com.mdyaipay.finance.api.reconcile;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 渠道对账 Dubbo 门面。
 * <p>不负责结算。异常映射为 {@code ApiResponse.code != 0}。</p>
 */
public interface ReconciliationFacade {

    /**
     * 执行对账。同一渠道、同一业务日的未处理批次会被覆盖。
     * <p>已处理批次拒绝重跑。幂等：相同输入再次执行得到同一覆盖结果。</p>
     */
    ApiResponse<ReconciliationBatchView> reconcile(ReconcileCommand command);

    /**
     * 查询已保存的批次。不存在时 code 为未找到。无副作用。
     */
    ApiResponse<ReconciliationBatchView> getBatch(String channel, String businessDate);
}
