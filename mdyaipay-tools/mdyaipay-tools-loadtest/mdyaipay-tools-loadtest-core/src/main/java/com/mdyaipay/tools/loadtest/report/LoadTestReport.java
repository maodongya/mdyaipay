package com.mdyaipay.tools.loadtest.report;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 一轮压测的汇总结果；JSON 导出字段与 CI 门禁脚本直接对应。
 *
 * @param latencyPercentilesMillis 键为分位点（如 {@code 0.99}），值为毫秒
 * @param errorSamples             截断后的错误摘要列表，见 {@link com.mdyaipay.tools.loadtest.model.MetricsProfile#maxErrorSamples()}
 */
public record LoadTestReport(
        String planName,
        String protocol,
        Instant startedAt,
        Instant finishedAt,
        long totalSamples,
        long successCount,
        long errorCount,
        double errorRate,
        double throughputRps,
        double minLatencyMillis,
        double maxLatencyMillis,
        double meanLatencyMillis,
        Map<Double, Double> latencyPercentilesMillis,
        List<String> errorSamples
) {
}
