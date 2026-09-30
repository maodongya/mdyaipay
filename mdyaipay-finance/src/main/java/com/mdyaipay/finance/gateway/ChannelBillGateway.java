package com.mdyaipay.finance.gateway;

import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;

import java.time.LocalDate;
import java.util.List;

/**
 * 向渠道拉取某一业务日的账单行。
 * <p>不负责文件解析，也不负责比对。</p>
 */
public interface ChannelBillGateway {

    /**
     * 拉取账单。无本地副作用。
     *
     * @param channel      渠道编码
     * @param businessDate 业务日
     */
    List<ChannelBillLine> pull(String channel, LocalDate businessDate);
}
