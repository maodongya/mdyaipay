package com.mdyaipay.tools.logging;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证共享日志配置包含控制台、滚动文件与 ECS 布局。
 */
class LoggingConfigurationTest {

    /** 共享配置必须提供稳定的 ECS 字段与两个输出端。 */
    @Test
    void shouldProvideEcsConsoleAndRollingFile() throws Exception {
        String xml = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/log4j2-spring.xml")).readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(xml.contains("EcsLayout"));
        assertTrue(xml.contains("serviceName"));
        assertTrue(xml.contains("Console"));
        assertTrue(xml.contains("RollingFile"));
        assertTrue(xml.contains("AdditionalField"));
        assertTrue(xml.contains("trace.id"));
        assertTrue(xml.contains("com.mdyaipay.tools.timetrace.report"));
    }
}
