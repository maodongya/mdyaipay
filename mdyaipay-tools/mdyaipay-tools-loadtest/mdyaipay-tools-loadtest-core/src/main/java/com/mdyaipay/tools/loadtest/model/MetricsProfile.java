package com.mdyaipay.tools.loadtest.model;

import java.util.List;

/**
 * 指标采集配置，对应场景文件 {@code metrics} 段。
 *
 * @param percentiles      延迟分位数列表，如 {@code 0.99} 表示 P99（毫秒）
 * @param warmupSeconds    预热时长（秒）；该阶段样本<strong>不</strong>进入最终 {@link com.mdyaipay.tools.loadtest.report.LoadTestReport}
 * @param recordErrors     是否收集失败消息摘要
 * @param maxErrorSamples  错误明细条数上限，防止高错误率时 OOM
 */
public record MetricsProfile(
        List<Double> percentiles,
        long warmupSeconds,
        boolean recordErrors,
        int maxErrorSamples
) {
    /** 与 {@code loadtest-design.md} 默认一致：P50/P90/P95/P99，预热 10s。 */
    public static MetricsProfile defaults() {
        return new MetricsProfile(List.of(0.5, 0.9, 0.95, 0.99), 10, true, 100);
    }
}
