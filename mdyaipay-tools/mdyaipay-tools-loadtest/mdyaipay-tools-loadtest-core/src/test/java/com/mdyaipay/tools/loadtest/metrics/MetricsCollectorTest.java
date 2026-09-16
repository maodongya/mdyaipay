package com.mdyaipay.tools.loadtest.metrics;

import com.mdyaipay.tools.loadtest.model.MetricsProfile;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCollectorTest {

    @Test
    void recordsPercentilesAndCounts() {
        MetricsCollector c = new MetricsCollector(MetricsProfile.defaults());
        for (int i = 1; i <= 100; i++) {
            c.record(new SampleOutcome(true, i * 1_000_000L, 200, null));
        }
        c.record(new SampleOutcome(false, 50_000_000L, 500, "boom"));

        assertEquals(101, c.totalSamples());
        assertEquals(100, c.successCount());
        assertEquals(1, c.errorCount());
        assertTrue(c.latencyPercentilesMillis().get(0.5) >= 50);
        assertEquals(1, c.errorSamples().size());
    }
}
