package com.mdyaipay.tools.loadtest.scenario;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScenarioParserTest {

    @Test
    void parsesGatewayScenario() throws Exception {
        Path path = Path.of("../scenarios/gateway-health.yaml").normalize();
        if (!path.toFile().exists()) {
            path = Path.of("mdyaipay-tools/mdyaipay-tools-loadtest/scenarios/gateway-health.yaml");
        }
        var plan = new ScenarioParser().parse(path);
        assertEquals("gateway-health", plan.name());
        assertEquals("http", plan.protocol());
        assertEquals(20, plan.loadProfile().threads());
    }
}
