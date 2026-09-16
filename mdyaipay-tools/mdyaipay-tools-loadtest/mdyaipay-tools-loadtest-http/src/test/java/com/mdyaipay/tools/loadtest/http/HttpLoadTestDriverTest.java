package com.mdyaipay.tools.loadtest.http;

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

class HttpLoadTestDriverTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/ok", exchange -> {
            byte[] body = "ok".getBytes();
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
    void executesGetAgainstLocalServer() throws Exception {
        LoadTestPlan plan = new LoadTestPlan(
                "http-test",
                "http",
                new LoadProfile(1, 1, 0, 0, null),
                MetricsProfile.defaults(),
                Map.of(
                        "url", "http://127.0.0.1:" + port + "/ok",
                        "method", "GET",
                        "expectStatus", List.of(200)
                ),
                ReportConfig.defaults()
        );
        var outcome = new HttpLoadTestDriver().execute(plan, new LoadTestRunContext(0, 1));
        assertTrue(outcome.success());
    }
}
