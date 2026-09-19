package com.mdyaipay.tools.timetrace;

import com.mdyaipay.tools.timetrace.sample.TraceCollaboratorService;
import com.mdyaipay.tools.timetrace.sample.TraceSampleService;
import com.mdyaipay.tools.timetrace.sample.TraceThresholdSampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdyaipay.tools.timetrace.autoconfigure.TimeTraceAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TimeTraceAspectTest.TestConfig.class)
class TimeTraceAspectTest {

    @Configuration
    @ImportAutoConfiguration({AopAutoConfiguration.class, TimeTraceAutoConfiguration.class})
    static class TestConfig {

        @Bean
        TraceCollaboratorService traceCollaboratorService() {
            return new TraceCollaboratorService();
        }

        @Bean
        TraceSampleService traceSampleService(TraceCollaboratorService traceCollaboratorService) {
            return new TraceSampleService(traceCollaboratorService);
        }

        @Bean
        TraceThresholdSampleService traceThresholdSampleService() {
            return new TraceThresholdSampleService();
        }
    }

    @Autowired
    private TraceSampleService sampleService;

    @Autowired
    private TraceThresholdSampleService thresholdSampleService;

    private final List<TimeTraceReport> reports = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TimeTraceSupport.setListener(reports::add);
    }

    @AfterEach
    void tearDown() {
        TimeTraceSupport.setListener(TimeTraceListener.LOG_TO_STDOUT);
        reports.clear();
    }

    @Test
    void tracesEntryMethod() {
        assertEquals(29, sampleService.run(2));

        assertEquals(1, reports.size());
        TimeTraceReport report = reports.get(0);
        assertEquals("sample-flow", report.getLabel());
        assertTrue(report.isSuccess());
        assertTrue(report.getTotalNanos() > 0);

        TimeTraceNode root = report.getRoot();
        assertTrue(root.getSignature().contains("run"));
        assertTrue(
                root.getChildren().stream().anyMatch(n -> n.getSignature().contains("TraceCollaboratorService.bump")),
                "跨 Bean 调用应出现在报告树");
    }

    @Test
    void skipsReportWhenTotalBelowThreshold() {
        thresholdSampleService.fastCall();
        assertTrue(reports.isEmpty(), "耗时低于 reportThresholdMillis 时不应输出报告");
    }

    @Test
    void formatterPrintsTree() {
        sampleService.run(1);
        String text = TimeTraceReportFormatter.format(reports.get(0));
        assertTrue(text.contains("[TimeTrace] sample-flow"));
        assertTrue(text.contains("total="));
        assertTrue(text.contains("self="));
    }
}
