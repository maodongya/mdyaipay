package com.mdyaipay.tools.loadtest.engine;

import com.mdyaipay.tools.loadtest.model.LoadProfile;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.MetricsProfile;
import com.mdyaipay.tools.loadtest.model.ReportConfig;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTestEngineTest {

    @Test
    void runsShortLoadWithFakeDriver() throws Exception {
        LoadTestPlan plan = new LoadTestPlan(
                "fake",
                "fake",
                new LoadProfile(4, 2, 0, 0, null),
                new MetricsProfile(List.of(0.99), 0, false, 0),
                Map.of(),
                new ReportConfig(List.of("console"), "target/test-reports")
        );
        LoadTestDriver driver = new LoadTestDriver() {
            @Override
            public String protocol() {
                return "fake";
            }

            @Override
            public SampleOutcome execute(LoadTestPlan p, LoadTestRunContext ctx) {
                return new SampleOutcome(true, 2_000_000L, 200, null);
            }
        };
        var report = new LoadTestEngine().run(plan, driver);
        assertTrue(report.totalSamples() > 0);
        assertTrue(report.throughputRps() > 0);
    }
}
