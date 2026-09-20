package com.mdyaipay.tools.loadtest.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 按 {@code formats} 分发到控制台、JSON、Markdown、HTML；文件名为 sanitized 后的 {@link LoadTestReport#planName()}。
 */
public final class CompositeReportExporter implements ReportExporter {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void export(LoadTestReport report, Path outputDir, List<String> formats) {
        /* 功能块：准备输出目录 — 相对路径相对于进程 cwd（CLI 通常在 cli 模块目录） */
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create output dir: " + outputDir, e);
        }
        String base = sanitize(report.planName());
        for (String format : formats) {
            switch (format.toLowerCase(Locale.ROOT)) {
                case "console" -> System.out.println(ConsoleReportFormatter.format(report));
                case "json" -> writeJson(report, outputDir.resolve(base + ".json"));
                case "markdown", "md" -> writeMarkdown(report, outputDir.resolve(base + ".md"));
                case "html" -> writeHtml(report, outputDir.resolve(base + ".html"));
                default -> System.err.println("Unknown report format: " + format);
            }
        }
    }

    private static void writeJson(LoadTestReport report, Path path) {
        try {
            JSON.writeValue(path.toFile(), report);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON report: " + path, e);
        }
    }

    private static void writeMarkdown(LoadTestReport report, Path path) {
        StringBuilder md = new StringBuilder();
        md.append("# Load Test: ").append(report.planName()).append("\n\n");
        md.append("- **Protocol**: ").append(report.protocol()).append("\n");
        md.append("- **Window**: ").append(report.startedAt()).append(" → ").append(report.finishedAt()).append("\n");
        md.append("- **Throughput**: ").append(String.format(Locale.ROOT, "%.2f", report.throughputRps())).append(" req/s\n");
        md.append("- **Error rate**: ").append(String.format(Locale.ROOT, "%.2f%%", report.errorRate() * 100)).append("\n\n");
        md.append("## Latency (ms)\n\n");
        md.append("| Metric | Value |\n|--------|------:|\n");
        md.append("| min | ").append(fmt(report.minLatencyMillis())).append(" |\n");
        md.append("| mean | ").append(fmt(report.meanLatencyMillis())).append(" |\n");
        md.append("| max | ").append(fmt(report.maxLatencyMillis())).append(" |\n");
        for (Map.Entry<Double, Double> e : report.latencyPercentilesMillis().entrySet()) {
            md.append("| P").append((int) (e.getKey() * 100)).append(" | ")
                    .append(fmt(e.getValue())).append(" |\n");
        }
        if (report.httpStatusCounts() != null && !report.httpStatusCounts().isEmpty()) {
            md.append("\n## HTTP status\n\n");
            md.append("| Status | Count |\n|--------|------:|\n");
            for (Map.Entry<Integer, Long> e : report.httpStatusCounts().entrySet()) {
                md.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n");
            }
        }
        if (!report.errorSamples().isEmpty()) {
            md.append("\n## Errors (sample)\n\n");
            for (String err : report.errorSamples()) {
                md.append("- ").append(err.replace("\n", " ")).append("\n");
            }
        }
        try {
            Files.writeString(path, md.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Markdown report: " + path, e);
        }
    }

    private static void writeHtml(LoadTestReport report, Path path) {
        StringBuilder rows = new StringBuilder();
        rows.append(row("Throughput (req/s)", fmt(report.throughputRps())));
        rows.append(row("Error rate", String.format(Locale.ROOT, "%.2f%%", report.errorRate() * 100)));
        rows.append(row("min / mean / max (ms)",
                fmt(report.minLatencyMillis()) + " / " + fmt(report.meanLatencyMillis()) + " / "
                        + fmt(report.maxLatencyMillis())));
        for (Map.Entry<Double, Double> e : report.latencyPercentilesMillis().entrySet()) {
            rows.append(row("P" + (int) (e.getKey() * 100), fmt(e.getValue()) + " ms"));
        }
        if (report.httpStatusCounts() != null && !report.httpStatusCounts().isEmpty()) {
            StringBuilder status = new StringBuilder();
            for (Map.Entry<Integer, Long> e : report.httpStatusCounts().entrySet()) {
                status.append(e.getKey()).append(": ").append(e.getValue()).append("; ");
            }
            rows.append(row("HTTP status", status.toString().trim()));
        }
        String html = """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"/>
                <title>Load Test %s</title>
                <style>
                body{font-family:system-ui,sans-serif;margin:2rem;color:#1a1a1a}
                table{border-collapse:collapse;margin-top:1rem}
                th,td{border:1px solid #ccc;padding:.5rem 1rem;text-align:left}
                th{background:#f5f5f5}
                </style></head><body>
                <h1>Load Test: %s</h1>
                <p><strong>Protocol:</strong> %s<br/>
                <strong>Window:</strong> %s &rarr; %s</p>
                <table><thead><tr><th>Metric</th><th>Value</th></tr></thead><tbody>
                %s
                </tbody></table>
                </body></html>
                """.formatted(
                report.planName(), report.planName(), report.protocol(),
                report.startedAt(), report.finishedAt(), rows);
        try {
            Files.writeString(path, html);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write HTML report: " + path, e);
        }
    }

    private static String row(String k, String v) {
        return "<tr><td>" + escape(k) + "</td><td>" + escape(v) + "</td></tr>\n";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;");
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
