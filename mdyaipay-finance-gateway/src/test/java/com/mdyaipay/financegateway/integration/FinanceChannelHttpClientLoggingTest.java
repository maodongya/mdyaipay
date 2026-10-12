package com.mdyaipay.financegateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渠道 HTTP 失败与超时事件日志。
 */
class FinanceChannelHttpClientLoggingTest {

    /** HTTP 5xx 记录 channel_http_failed。 */
    @Test
    void shouldLogChannelHttpFailed() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/channel/collect", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        try {
            FinanceChannelHttpClient client = newClient(server.getAddress().getPort());
            List<String> messages = LogMessageCapture.capture(FinanceChannelHttpClient.class, () ->
                    client.collect(new ChannelCollectCommand("O1", 100L, "MOCK", "QUICK_COLLECTION", 1L)));
            assertTrue(messages.stream().anyMatch(line ->
                    line.contains(AlertEvents.failure(AlertEvents.CHANNEL_HTTP_FAILED))));
        } finally {
            server.stop(0);
        }
    }

    /** 读取超时记录 channel_http_timeout。 */
    @Test
    void shouldLogChannelHttpTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/channel/collect", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        try {
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build();
            FinanceChannelHttpClient client = new FinanceChannelHttpClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    new ObjectMapper(),
                    httpClient,
                    Duration.ofMillis(200));
            List<String> messages = LogMessageCapture.capture(FinanceChannelHttpClient.class, () ->
                    client.collect(new ChannelCollectCommand("O2", 100L, "MOCK", "QUICK_COLLECTION", 1L)));
            assertTrue(messages.stream().anyMatch(line ->
                    line.contains(AlertEvents.failure(AlertEvents.CHANNEL_HTTP_TIMEOUT))));
        } finally {
            server.stop(0);
        }
    }

    /** 指向本地 HTTP 服务的默认超时客户端。 */
    private static FinanceChannelHttpClient newClient(int port) {
        return new FinanceChannelHttpClient(
                "http://127.0.0.1:" + port,
                new ObjectMapper(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }
}
