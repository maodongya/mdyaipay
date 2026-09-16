package com.mdyaipay.tools.loadtest.model;

import java.util.List;

/**
 * 报告导出配置，对应场景文件 {@code report} 段。
 *
 * @param formats   如 {@code console}、{@code json}、{@code markdown}、{@code html}
 * @param outputDir 文件报告输出目录（相对或绝对路径）；{@code console} 仍打印到 stdout
 */
public record ReportConfig(
        List<String> formats,
        String outputDir
) {
    public static ReportConfig defaults() {
        return new ReportConfig(List.of("console"), "target/loadtest-reports");
    }
}
