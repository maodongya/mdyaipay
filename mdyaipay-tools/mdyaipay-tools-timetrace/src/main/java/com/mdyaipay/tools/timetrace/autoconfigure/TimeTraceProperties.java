package com.mdyaipay.tools.timetrace.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code mdyaipay.timetrace.*} 配置项。
 */
@ConfigurationProperties(prefix = "mdyaipay.timetrace")
public class TimeTraceProperties {

    /**
     * 是否注册 {@link com.mdyaipay.tools.timetrace.TimeTraceAspect} Bean。
     */
    private boolean enabled = true;

    /**
     * 报告输出：{@code log4j} 写入 {@link com.mdyaipay.tools.timetrace.TimeTraceLog4j#REPORT_LOGGER_NAME}；
     * {@code stdout} 写入标准输出。
     */
    private ReportSink reportSink = ReportSink.LOG4J;

    /**
     * TimeTrace 专用日志文件路径（供应用侧 log4j2 配置引用，如 {@code ${spring:mdyaipay.timetrace.log-file}}）。
     */
    private String logFile = "logs/timetrace.log";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ReportSink getReportSink() {
        return reportSink;
    }

    public void setReportSink(ReportSink reportSink) {
        this.reportSink = reportSink != null ? reportSink : ReportSink.LOG4J;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile != null && !logFile.isBlank() ? logFile : "logs/timetrace.log";
    }

    public enum ReportSink {
        STDOUT,
        LOG4J
    }
}
