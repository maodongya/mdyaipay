package com.mdyaipay.tools.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具：平台内部以「分」为 long 存储，对外展示或费率计算可用 {@link BigDecimal}（元）。
 */
public final class MoneyUtils {

    public static final int FEN_PER_YUAN = 100;
    public static final RoundingMode ROUND = RoundingMode.HALF_UP;
    public static final int MONEY_SCALE = 2;

    private MoneyUtils() {
    }

    public static long requirePositiveFen(long amountFen) {
        if (amountFen <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        return amountFen;
    }

    public static long yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            throw new IllegalArgumentException("yuan must not be null");
        }
        return yuan.multiply(BigDecimal.valueOf(FEN_PER_YUAN))
                .setScale(0, ROUND)
                .longValueExact();
    }

    public static BigDecimal fenToYuan(long amountFen) {
        return BigDecimal.valueOf(amountFen)
                .divide(BigDecimal.valueOf(FEN_PER_YUAN), MONEY_SCALE, ROUND);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUND);
    }
}
