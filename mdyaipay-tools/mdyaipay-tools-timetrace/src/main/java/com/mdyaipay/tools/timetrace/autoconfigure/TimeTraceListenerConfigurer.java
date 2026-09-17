package com.mdyaipay.tools.timetrace.autoconfigure;

import com.mdyaipay.tools.timetrace.TimeTraceListener;
import com.mdyaipay.tools.timetrace.TimeTraceLog4j;
import com.mdyaipay.tools.timetrace.TimeTraceSupport;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在根 {@link ContextRefreshedEvent} 后注册全局 {@link TimeTraceListener}，确保 Log4j2 已完成 Spring 配置。
 */
final class TimeTraceListenerConfigurer implements ApplicationListener<ContextRefreshedEvent> {

    private final TimeTraceProperties properties;
    private final AtomicBoolean registered = new AtomicBoolean();

    TimeTraceListenerConfigurer(TimeTraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        TimeTraceListener listener = switch (properties.getReportSink()) {
            case STDOUT -> TimeTraceListener.LOG_TO_STDOUT;
            case LOG4J -> TimeTraceLog4j.defaultListener();
        };
        TimeTraceSupport.setListener(listener);
    }
}
