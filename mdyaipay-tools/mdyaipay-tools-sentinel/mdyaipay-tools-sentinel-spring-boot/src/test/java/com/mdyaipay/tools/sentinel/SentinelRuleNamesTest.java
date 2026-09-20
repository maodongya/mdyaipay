package com.mdyaipay.tools.sentinel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelRuleNames} 单测。
 */
class SentinelRuleNamesTest {

    /**
     * local 资源名格式。
     */
    @Test
    void localResource_formatsPrefix() {
        assertEquals("local:collect", SentinelRuleNames.localResource("collect"));
    }

    /**
     * cluster 前缀解析 ruleId。
     */
    @Test
    void clusterRuleId_parsesPrefix() {
        assertEquals(
                "gateway-payment-collect",
                SentinelRuleNames.clusterRuleId("cluster:gateway-payment-collect").orElseThrow());
        assertTrue(SentinelRuleNames.clusterRuleId("local:x").isEmpty());
    }
}
