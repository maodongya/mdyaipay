package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.memory.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 内存 backend 自动配置冒烟测。
 */
@SpringBootTest(classes = RateLimitAutoConfigurationMemoryTest.TestApp.class)
@ImportAutoConfiguration(RateLimitAutoConfiguration.class)
@TestPropertySource(properties = {
        "mdyaipay.ratelimit.enabled=true",
        "mdyaipay.ratelimit.backend=memory"
})
class RateLimitAutoConfigurationMemoryTest {

    @Configuration
    static class TestApp {
    }

    @Autowired
    private RateLimiter rateLimiter;

    /**
     * 默认装配内存限流器。
     */
    @Test
    void wiresInMemoryRateLimiter() {
        assertNotNull(rateLimiter);
        assertInstanceOf(InMemoryRateLimiter.class, rateLimiter);
    }
}
