package com.mdyaipay.payment.domain.collect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 收单业务日：成功时刻的 {@code updatedAt} 换算到上海日历日。
 * <p>财务对账与支付查询使用同一时区。</p>
 */
public final class CollectBusinessDay {

    /** 业务日时区。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private CollectBusinessDay() {
    }

    /**
     * 业务日当天起始（含）。无副作用。
     */
    public static Instant startInclusive(LocalDate businessDate) {
        return businessDate.atStartOfDay(ZONE).toInstant();
    }

    /**
     * 业务日次日起始（不含）。无副作用。
     */
    public static Instant endExclusive(LocalDate businessDate) {
        return businessDate.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
