package com.mdyaipay.tools.loadtest.spi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTestDriverRegistryTest {

    @Test
    void emptyRegistryWhenNoDriversOnClasspath() {
        var registry = new LoadTestDriverRegistry(List.of());
        assertTrue(registry.all().isEmpty());
    }
}
