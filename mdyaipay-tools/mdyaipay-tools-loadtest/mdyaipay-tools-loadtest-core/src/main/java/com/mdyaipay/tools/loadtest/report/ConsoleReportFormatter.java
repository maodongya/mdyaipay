package com.mdyaipay.tools.loadtest.report;

import java.util.Locale;
import java.util.Map;

/** 人类可读的控制台摘要，供 CI 日志快速浏览。 */
public final class ConsoleReportFormatter {

    private ConsoleReportFormatter() {
    }

    public static String format(LoadTestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Load Test Report ===").append(System.lineSeparator());
        sb.append("Plan: ").append(report.planName()).append(" (").append(report.protocol()).append(')')
                .append(System.lineSeparator());
        sb.append(String.format(Locale.ROOT, "Duration: %s -> %s%n", report.startedAt(), report.finishedAt()));
        sb.append(String.format(Locale.ROOT, "Samples: %d  Success: %d  Errors: %d  Error rate: %.2f%%%n",
                report.totalSamples(), report.successCount(), report.errorCount(), report.errorRate() * 100));
        sb.append(String.format(Locale.ROOT, "Throughput: %.2f req/s%n", report.throughputRps()));
        sb.append(String.format(Locale.ROOT, "Latency ms — min: %.2f  mean: %.2f  max: %.2f%n",
                report.minLatencyMillis(), report.meanLatencyMillis(), report.maxLatencyMillis()));
        for (Map.Entry<Double, Double> e : report.latencyPercentilesMillis().entrySet()) {
            sb.append(String.format(Locale.ROOT, "  P%.0f: %.2f ms%n", e.getKey() * 100, e.getValue()));
        }
        if (!report.errorSamples().isEmpty()) {
            sb.append("Error samples:").append(System.lineSeparator());
            report.errorSamples().stream().limit(10).forEach(e -> sb.append("  - ").append(e).append(System.lineSeparator()));
        }
        return sb.toString();
    }
}
