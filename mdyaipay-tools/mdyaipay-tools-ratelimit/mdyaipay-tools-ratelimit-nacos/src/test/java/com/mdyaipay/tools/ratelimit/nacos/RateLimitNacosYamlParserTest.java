package com.mdyaipay.tools.ratelimit.nacos;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RateLimitNacosYamlParser} 单测。
 */
class RateLimitNacosYamlParserTest {

    /**
     * 解析 rules 与 default-policy。
     */
    @Test
    void parse_readsRules() {
        String yaml =
                """
                enabled: true
                default-policy:
                  limit: 20
                rules:
                  - id: demo-rule
                    match:
                      path: /api/v1/demo
                    policy:
                      limit: 10
                """;
        RateLimitProperties properties = RateLimitNacosYamlParser.parse(yaml).orElseThrow();
        assertEquals(1, properties.getRules().size());
        assertEquals("demo-rule", properties.getRules().get(0).getId());
        assertEquals(20L, properties.getDefaultPolicy().getLimit());
    }

    /**
     * 空内容返回 empty。
     */
    @Test
    void parse_blankReturnsEmpty() {
        assertTrue(RateLimitNacosYamlParser.parse("  ").isEmpty());
    }
}
