package com.mdyaipay.tools.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * 捕获指定类型 logger 在回调期间输出的格式化消息。
 * <p>
 * 不负责改 log4j2 配置文件；只给单测断言稳定 event 文本。
 */
public final class LogMessageCapture {

    /**
     * 禁止实例化。
     */
    private LogMessageCapture() {
    }

    /**
     * 临时挂上 WriterAppender，收集消息后按行返回。
     * <p>
     * 前置条件：{@code type} 与 {@code action} 均非空。幂等：每次独立 appender，结束后移除。
     *
     * @param type   与业务类同名的 logger
     * @param action 会打日志的回调
     * @return 非空消息行
     */
    public static List<String> capture(Class<?> type, Runnable action) {
        if (type == null || action == null) {
            throw new IllegalArgumentException("type and action must not be null");
        }
        LoggerContext context = LoggerContext.getContext(false);
        Logger logger = (Logger) context.getLogger(type.getName());
        StringWriter writer = new StringWriter();
        WriterAppender appender = WriterAppender.newBuilder()
                .setName("log-capture-" + type.getName() + "-" + System.nanoTime())
                .setTarget(writer)
                .setLayout(PatternLayout.newBuilder().withPattern("%m%n").build())
                .build();
        appender.start();
        Level previous = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            action.run();
            return Arrays.stream(writer.toString().split("\\R"))
                    .filter(line -> !line.isBlank())
                    .toList();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
            logger.setLevel(previous);
        }
    }
}
