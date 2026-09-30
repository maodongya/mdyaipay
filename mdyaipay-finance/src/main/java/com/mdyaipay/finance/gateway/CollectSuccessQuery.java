package com.mdyaipay.finance.gateway;

import com.mdyaipay.finance.domain.reconcile.LocalCollectSuccess;

import java.time.LocalDate;
import java.util.List;

/**
 * 读取某一渠道、某一业务日已成功且带渠道交易号的收单。
 * <p>不负责比对。</p>
 */
public interface CollectSuccessQuery {

    /**
     * 按渠道与业务日列出成功收单。无副作用。
     *
     * @param channel      渠道编码
     * @param businessDate 业务日（Asia/Shanghai）
     */
    List<LocalCollectSuccess> list(String channel, LocalDate businessDate);
}
