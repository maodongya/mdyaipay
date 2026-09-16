package com.mdyaipay.tools.loadtest.report;

import java.nio.file.Path;
import java.util.List;

/**
 * 压测报告导出 SPI；默认实现见 {@link CompositeReportExporter}。
 *
 * @param formats {@code console} 写 stdout，其余格式写 {@code outputDir} 下以场景名命名的文件
 */
public interface ReportExporter {

    void export(LoadTestReport report, Path outputDir, List<String> formats);
}
