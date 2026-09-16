package com.mdyaipay.gateway.id;

import com.mdyaipay.tools.id.SnowflakeIdGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 独立 HTTP 微服务：基于雪花算法生成全局唯一 ID。
 *
 * <ul>
 *     <li>{@code GET /health}</li>
 *     <li>{@code GET /api/v1/ids/next} — 生成单个 ID</li>
 *     <li>{@code POST /api/v1/ids/batch} — 批量生成，body: {@code {"count":10}}</li>
 * </ul>
 */
public final class IdGeneratorHttpService {

    private static final int DEFAULT_PORT = 8042;
    private static final int MAX_BATCH = 1000;

    private final ObjectMapper json = new ObjectMapper();
    private final SnowflakeIdGenerator idGenerator;
    private final HttpServer server;

    public IdGeneratorHttpService(int port, long workerId, long datacenterId) throws IOException {
        this.idGenerator = new SnowflakeIdGenerator(workerId, datacenterId);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::dispatch);
        this.server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    public SnowflakeIdGenerator idGenerator() {
        return idGenerator;
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/health".equals(path) && "GET".equalsIgnoreCase(method)) {
                writeText(exchange, 200, "OK");
                return;
            }
            if ("/api/v1/ids/next".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleNext(exchange);
                return;
            }
            if ("/api/v1/ids/batch".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleBatch(exchange);
                return;
            }
            writeJsonError(exchange, 404, "not found");
        } catch (IllegalArgumentException ex) {
            writeJsonError(exchange, 400, ex.getMessage());
        }
    }

    private void handleNext(HttpExchange exchange) throws IOException {
        long id = idGenerator.nextId();
        ObjectNode body = json.createObjectNode();
        body.put("id", String.valueOf(id));
        body.put("idLong", id);
        writeJson(exchange, 200, body);
    }

    private void handleBatch(HttpExchange exchange) throws IOException {
        int count = parseBatchCount(exchange);
        ArrayNode ids = json.createArrayNode();
        for (int i = 0; i < count; i++) {
            long id = idGenerator.nextId();
            ObjectNode item = json.createObjectNode();
            item.put("id", String.valueOf(id));
            item.put("idLong", id);
            ids.add(item);
        }
        ObjectNode body = json.createObjectNode();
        body.put("count", count);
        body.set("ids", ids);
        writeJson(exchange, 200, body);
    }

    private int parseBatchCount(HttpExchange exchange) throws IOException {
        String raw = readRequestBody(exchange);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("body requires field \"count\"");
        }
        var node = json.readTree(raw);
        if (!node.has("count") || !node.get("count").canConvertToInt()) {
            throw new IllegalArgumentException("body requires positive integer field \"count\"");
        }
        int count = node.get("count").asInt();
        if (count < 1 || count > MAX_BATCH) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_BATCH);
        }
        return count;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeJson(HttpExchange exchange, int status, ObjectNode body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void writeJsonError(HttpExchange exchange, int status, String message) throws IOException {
        ObjectNode err = json.createObjectNode();
        err.put("error", message);
        writeJson(exchange, status, err);
    }

    private void writeText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = parseInt(System.getenv("ID_GENERATOR_PORT"), DEFAULT_PORT);
        long workerId = parseLong(System.getenv("ID_GENERATOR_WORKER_ID"), 1L);
        long datacenterId = parseLong(System.getenv("ID_GENERATOR_DATACENTER_ID"), 1L);

        IdGeneratorHttpService service = new IdGeneratorHttpService(port, workerId, datacenterId);
        service.start();
        System.out.println("id generator listening on http://localhost:" + port);
        System.out.println("GET  /health");
        System.out.println("GET  /api/v1/ids/next");
        System.out.println("POST /api/v1/ids/batch  body: {\"count\":10}");
    }

    private static int parseInt(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long parseLong(String raw, long defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
