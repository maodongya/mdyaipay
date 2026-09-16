package com.mdyaipay.tools.loadtest.springcloud;

import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadProfile;
import com.mdyaipay.tools.loadtest.model.MetricsProfile;
import com.mdyaipay.tools.loadtest.model.ReportConfig;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringCloudLoadTestDriverTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/ping", exchange -> {
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loadBalancerRoundRobin() throws Exception {
        String base = "http://127.0.0.1:" + port;
        LoadTestPlan plan = new LoadTestPlan(
                "sc-lb",
                "springcloud",
                new LoadProfile(1, 1, 0, 0, null),
                MetricsProfile.defaults(),
                Map.of(
                        "mode", "loadbalancer",
                        "instances", List.of(base),
                        "path", "/api/ping",
                        "method", "GET",
                        "expectStatus", List.of(200)
                ),
                ReportConfig.defaults()
        );
        var outcome = new SpringCloudLoadTestDriver().execute(plan, new LoadTestRunContext(0, 3));
        assertTrue(outcome.success());
    }
}
