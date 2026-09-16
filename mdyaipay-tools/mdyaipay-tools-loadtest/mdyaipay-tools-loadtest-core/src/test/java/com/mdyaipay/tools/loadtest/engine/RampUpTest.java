package com.mdyaipay.tools.loadtest.engine;

import com.mdyaipay.tools.loadtest.model.LoadProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RampUpTest {

    @Test
    void linearRampDelay() {
        LoadProfile load = new LoadProfile(10, 60, 10, 0, null);
        assertEquals(0, LoadTestEngine.computeRampDelayMillis(load, 0, 10));
        assertEquals(5000, LoadTestEngine.computeRampDelayMillis(load, 5, 10));
    }
}
