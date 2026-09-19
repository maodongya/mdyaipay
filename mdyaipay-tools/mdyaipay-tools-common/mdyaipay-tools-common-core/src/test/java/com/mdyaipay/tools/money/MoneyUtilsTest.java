package com.mdyaipay.tools.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyUtilsTest {

    @Test
    void yuanToFenAndBack() {
        assertEquals(100L, MoneyUtils.yuanToFen(new BigDecimal("1.00")));
        assertEquals(0, MoneyUtils.fenToYuan(100).compareTo(new BigDecimal("1.00")));
    }

    @Test
    void requirePositiveFenRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.requirePositiveFen(0));
        assertEquals(1L, MoneyUtils.requirePositiveFen(1));
    }
}
