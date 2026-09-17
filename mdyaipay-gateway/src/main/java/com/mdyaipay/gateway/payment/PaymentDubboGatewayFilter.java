package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.gateway.dubbo.PaymentGatewayDubboClient;
import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.tools.model.ApiResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支付/代扣/代付经 Dubbo 调 payment（collect 加密收单由 {@link MerchantSignedCollectGatewayFilter} 单独处理）。
 */
@Component
public class PaymentDubboGatewayFilter implements GlobalFilter, Ordered {

    private static final Pattern PAYMENT_GET = Pattern.compile("^/api/v1/payments/([^/]+)$");
    private static final Pattern PAYMENT_CONFIRM = Pattern.compile("^/api/v1/payments/([^/]+)/channel-confirm$");

    private final PaymentGatewayDubboClient paymentClient;
    private final ObjectMapper json;

    public PaymentDubboGatewayFilter(PaymentGatewayDubboClient paymentClient, ObjectMapper json) {
        this.paymentClient = paymentClient;
        this.json = json;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        if (method == null) {
            return chain.filter(exchange);
        }

        if ("/api/v1/payments/collect".equals(path) && HttpMethod.POST.equals(method)) {
            return chain.filter(exchange);
        }
        if (!matchesPaymentApi(path, method)) {
            return chain.filter(exchange);
        }

        /* 功能块：读 body 并阻塞式 Dubbo — reactive 线程上只做 I/O 边界，业务在 boundedElastic */
        if (HttpMethod.POST.equals(method)) {
            return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        byte[] raw = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(raw);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(raw, StandardCharsets.UTF_8);
                        return Mono.fromCallable(() -> invoke(path, method.name(), body))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(responseBytes -> writeJson(exchange, 200, responseBytes));
                    })
                    .onErrorResume(IllegalArgumentException.class,
                            ex -> writeJson(exchange, 400, errorBody(ex.getMessage())));
        }

        return Mono.fromCallable(() -> invoke(path, method.name(), ""))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(responseBytes -> writeJson(exchange, 200, responseBytes))
                .onErrorResume(IllegalArgumentException.class,
                        ex -> writeJson(exchange, 400, errorBody(ex.getMessage())));
    }

    private byte[] invoke(String path, String method, String body) throws Exception {
        /* 功能块：路由到 Dubbo — 与对外 REST 路径一一对应，collect 不在此处理 */
        if ("/api/v1/withholds".equals(path) && HttpMethod.POST.name().equals(method)) {
            JsonNode root = json.readTree(body);
            var response = paymentClient.createWithhold(new CreateWithholdCommand(
                    text(root, "deductionNo"),
                    text(root, "agreementNo"),
                    root.get("amount").asLong(),
                    text(root, "channel")));
            return toBytes(response);
        }
        if ("/api/v1/payouts".equals(path) && HttpMethod.POST.name().equals(method)) {
            JsonNode root = json.readTree(body);
            var response = paymentClient.createPayout(new CreatePayoutCommand(
                    text(root, "payoutNo"),
                    root.get("amount").asLong(),
                    text(root, "channel"),
                    text(root, "payeeRef")));
            return toBytes(response);
        }
        Matcher getPay = PAYMENT_GET.matcher(path);
        if (getPay.matches() && HttpMethod.GET.name().equals(method)) {
            return toBytes(paymentClient.getPayment(getPay.group(1)));
        }
        Matcher confirm = PAYMENT_CONFIRM.matcher(path);
        if (confirm.matches() && HttpMethod.POST.name().equals(method)) {
            JsonNode root = body.isBlank() ? json.createObjectNode() : json.readTree(body);
            boolean success = root.has("success") && root.get("success").asBoolean();
            return toBytes(paymentClient.confirmChannelPayment(new ChannelConfirmCommand(confirm.group(1), success)));
        }
        throw new IllegalArgumentException("unsupported payment path");
    }

    private static boolean matchesPaymentApi(String path, HttpMethod method) {
        if ("/api/v1/withholds".equals(path) && HttpMethod.POST.equals(method)) {
            return true;
        }
        if ("/api/v1/payouts".equals(path) && HttpMethod.POST.equals(method)) {
            return true;
        }
        if (PAYMENT_GET.matcher(path).matches() && HttpMethod.GET.equals(method)) {
            return true;
        }
        return PAYMENT_CONFIRM.matcher(path).matches() && HttpMethod.POST.equals(method);
    }

    private byte[] toBytes(ApiResponse<?> response) throws Exception {
        if (response.getCode() != 0) {
            throw new IllegalArgumentException(response.getMessage());
        }
        return json.writeValueAsBytes(response.getData());
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText().isBlank()) {
            throw new IllegalArgumentException("missing " + field);
        }
        return node.asText();
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, int status, byte[] body) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] errorBody(String message) {
        try {
            return json.writeValueAsBytes(java.util.Map.of("error", message));
        } catch (Exception ex) {
            return ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
