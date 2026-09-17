package com.mdyaipay.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对外 HTTP 网关（JDK HttpServer）：ID 本地生成；商户转发 user；支付请用 Spring {@code MdyaipayGatewayApplication}（Dubbo）。
 */
public class MdyaipayGatewayServer {

    private static final int DEFAULT_PORT = 8041;
    private static final int MAX_ID_BATCH = 1000;
    private static final Pattern PAYMENT_GET = Pattern.compile("^/api/v1/payments/([^/]+)$");
    private static final Pattern PAYMENT_CONFIRM = Pattern.compile("^/api/v1/payments/([^/]+)/channel-confirm$");
    private static final Pattern MERCHANT_API = Pattern.compile("^/api/v1/merchants(/.*)?$");

    private final ObjectMapper json = new ObjectMapper();
    private final HttpServer server;
    private final UserBackendClient userBackend;
    private final SnowflakeIdGenerator idGenerator;

    public MdyaipayGatewayServer(int port) throws IOException {
        this(port, 1L, 1L);
    }

    public MdyaipayGatewayServer(int port, long workerId, long datacenterId) throws IOException {
        String userBase = System.getenv("USER_BASE_URL");
        String resolvedUserBase = userBase != null ? userBase : "http://127.0.0.1:8082";
        this.userBackend = new UserBackendClient(resolvedUserBase);
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

    private void dispatch(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/health".equals(path) && "GET".equalsIgnoreCase(method)) {
                writeText(exchange, 200, "OK");
                return;
            }

            if ("/api/v1/payments/collect".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleSignedCollect(exchange);
                return;
            }

            Matcher getPay = PAYMENT_GET.matcher(path);
            if (getPay.matches() && "GET".equalsIgnoreCase(method)) {
                proxyPayment(exchange, method, path);
                return;
            }

            Matcher confirm = PAYMENT_CONFIRM.matcher(path);
            if (confirm.matches() && "POST".equalsIgnoreCase(method)) {
                proxyPayment(exchange, method, path);
                return;
            }

            if ("/api/v1/withholds".equals(path) && "POST".equalsIgnoreCase(method)) {
                proxyPayment(exchange, method, path);
                return;
            }

            if ("/api/v1/payouts".equals(path) && "POST".equalsIgnoreCase(method)) {
                proxyPayment(exchange, method, path);
                return;
            }

            if (MERCHANT_API.matcher(path).matches()) {
                proxyUser(exchange, method, path);
                return;
            }

            if ("/api/v1/ids/next".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleNextId(exchange);
                return;
            }

            if ("/api/v1/ids/batch".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleBatchIds(exchange);
                return;
            }

            writeJsonError(exchange, 404, "not found");
        } catch (IllegalArgumentException ex) {
            writeJsonError(exchange, 400, ex.getMessage());
        } catch (IOException ex) {
            writeJsonError(exchange, 502, "backend unavailable: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            writeJsonError(exchange, 502, "backend interrupted");
        }
    }

    private void handleSignedCollect(HttpExchange exchange) throws IOException {
        writeJsonError(exchange, 501,
                "payment HTTP removed; use MdyaipayGatewayApplication (Dubbo + ZK) for encrypted collect");
    }

    private void proxyUser(HttpExchange exchange, String method, String path)
            throws IOException, InterruptedException {
        String query = exchange.getRequestURI().getRawQuery();
        String forwardPath = query == null || query.isBlank() ? path : path + "?" + query;
        String body = "GET".equalsIgnoreCase(method) ? null : readRequestBody(exchange);
        PaymentBackendClient.BackendResponse backend = userBackend.forward(method, forwardPath, body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(backend.statusCode(), backend.body().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(backend.body());
        }
    }

    private void proxyPayment(HttpExchange exchange, String method, String path) throws IOException {
        writeJsonError(exchange, 501,
                "payment HTTP API removed; run MdyaipayGatewayApplication (Dubbo via ZK)");
    }

    private void handleNextId(HttpExchange exchange) throws IOException {
        long id = idGenerator.nextId();
        ObjectNode body = json.createObjectNode();
        body.put("id", String.valueOf(id));
        body.put("idLong", id);
        writeJson(exchange, 200, body);
    }

    private void handleBatchIds(HttpExchange exchange) throws IOException {
        JsonNode body = readJsonBody(exchange);
        if (!body.has("count") || !body.get("count").canConvertToInt()) {
            writeJsonError(exchange, 400, "body requires positive integer field \"count\"");
            return;
        }
        int count = body.get("count").asInt();
        if (count < 1 || count > MAX_ID_BATCH) {
            writeJsonError(exchange, 400, "count must be between 1 and " + MAX_ID_BATCH);
            return;
        }
        var ids = json.createArrayNode();
        for (int i = 0; i < count; i++) {
            long id = idGenerator.nextId();
            ObjectNode item = json.createObjectNode();
            item.put("id", String.valueOf(id));
            item.put("idLong", id);
            ids.add(item);
        }
        ObjectNode resp = json.createObjectNode();
        resp.put("count", count);
        resp.set("ids", ids);
        writeJson(exchange, 200, resp);
    }

    private JsonNode readJsonBody(HttpExchange exchange) throws IOException {
        String raw = readRequestBody(exchange);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("empty body");
        }
        try {
            return json.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid json");
        }
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
        int port = parsePort(System.getenv("MDYAIPAY_GATEWAY_PORT"), DEFAULT_PORT);
        long workerId = parseLong(System.getenv("ID_GENERATOR_WORKER_ID"), 1L);
        long datacenterId = parseLong(System.getenv("ID_GENERATOR_DATACENTER_ID"), 1L);
        MdyaipayGatewayServer app = new MdyaipayGatewayServer(port, workerId, datacenterId);
        app.start();
        System.out.println("mdyaipay gateway listening on http://localhost:" + port);
        System.out.println("USER_BASE_URL -> user service (default http://127.0.0.1:8082)");
        System.out.println("GET  /health");
        System.out.println("GET  /api/v1/ids/next");
        System.out.println("POST /api/v1/ids/batch  body: {\"count\":10}");
        System.out.println("payment APIs -> 501 (use MdyaipayGatewayApplication + Dubbo)");
        System.out.println("/api/v1/merchants/**  (proxied to user)");
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

    private static int parsePort(String raw, int defaultPort) {
        if (raw == null || raw.isBlank()) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultPort;
        }
    }
}
