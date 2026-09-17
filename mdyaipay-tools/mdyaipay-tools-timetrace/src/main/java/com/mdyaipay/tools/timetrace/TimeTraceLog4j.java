package com.mdyaipay.tools.timetrace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * {@link TimeTraceListener} 与 Log4j2 的桥接；由 {@code com.mdyaipay.tools.timetrace.report} Logger 写入独立文件。
 */
public final class TimeTraceLog4j {

    /** Log4j2 中 TimeTrace 报告专用 Logger 名，便于在 {@code log4j2.xml} 中单独落盘。 */
    public static final String REPORT_LOGGER_NAME = "com.mdyaipay.tools.timetrace.report";

    private TimeTraceLog4j() {
    }

    public static Logger reportLogger() {
        return LogManager.getLogger(REPORT_LOGGER_NAME);
    }

    public static TimeTraceListener listener(Logger logger) {
        Objects.requireNonNull(logger, "logger must not be null");
        return report -> logger.info("{}", TimeTraceReportFormatter.format(report));
    }

    public static TimeTraceListener defaultListener() {
        return listener(reportLogger());
    }
}
