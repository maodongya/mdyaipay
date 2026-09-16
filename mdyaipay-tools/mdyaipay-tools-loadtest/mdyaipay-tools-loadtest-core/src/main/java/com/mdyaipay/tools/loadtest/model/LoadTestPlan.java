package com.mdyaipay.tools.loadtest.model;

import java.util.Map;

/**
 * 压测场景根对象：由 {@link com.mdyaipay.tools.loadtest.scenario.ScenarioParser} 从 YAML/JSON 构建。
 *
 * @param name            场景名，用于报告文件名与控制台标题
 * @param protocol        驱动协议，见 {@link com.mdyaipay.tools.loadtest.spi.LoadTestDriver#protocol()}
 * @param loadProfile     并发与时长
 * @param metricsProfile  分位数、预热、错误采样上限
 * @param target          协议相关参数（URL、Dubbo 接口等），由各驱动自行解读
 * @param reportConfig    导出格式与目录；{@code null} 时使用 {@link ReportConfig#defaults()}
 */
public record LoadTestPlan(
        String name,
        String protocol,
        LoadProfile loadProfile,
        MetricsProfile metricsProfile,
        Map<String, Object> target,
        ReportConfig reportConfig
) {
    public LoadTestPlan {
        if (reportConfig == null) {
            reportConfig = ReportConfig.defaults();
        }
    }
}
