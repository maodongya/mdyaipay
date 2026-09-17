package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.gateway.dubbo.PaymentGatewayClient;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
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

/**
 * POST {@code /api/v1/payments/collect}：商户加密报文 → Dubbo 查 user 凭证 → 验签解密 → Dubbo 调 payment 收单。
 */
@Component
public class MerchantSignedCollectGatewayFilter implements GlobalFilter, Ordered {

    private static final String COLLECT_PATH = "/api/v1/payments/collect";

    private final OpenApiCredentialResolver credentialResolver;
    private final PaymentGatewayClient paymentClient;
    private final MerchantSignedCollectProcessor processor;
    private final ObjectMapper json;

    public MerchantSignedCollectGatewayFilter(
            OpenApiCredentialResolver credentialResolver,
            PaymentGatewayClient paymentClient,
            ObjectMapper json) {
        this.credentialResolver = credentialResolver;
        this.paymentClient = paymentClient;
        this.json = json;
        this.processor = new MerchantSignedCollectProcessor(json);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!HttpMethod.POST.equals(request.getMethod()) || !COLLECT_PATH.equals(request.getURI().getPath())) {
            return chain.filter(exchange);
        }
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] raw = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(raw);
                    DataBufferUtils.release(dataBuffer);
                    String body = new String(raw, StandardCharsets.UTF_8);
                    return Mono.fromCallable(() -> collectViaDubbo(body))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(responseBytes -> writeOk(exchange, responseBytes));
                })
                .onErrorResume(MerchantSignedCollectException.class, ex -> writeError(exchange, ex));
    }

    private byte[] collectViaDubbo(String encryptedBody) throws Exception {
        /* 功能块：解析 appKey — 商户身份入口，缺失直接 400 */
        JsonNode root = json.readTree(encryptedBody);
        JsonNode appKeyNode = root.get("appKey");
        if (appKeyNode == null || appKeyNode.asText().isBlank()) {
            appKeyNode = root.get("app_key");
        }
        if (appKeyNode == null || appKeyNode.asText().isBlank()) {
            throw MerchantSignedCollectException.badRequest("appKey required");
        }

        /* 功能块：凭证与验签解密 — 内网 Dubbo 取 secret，明文 payload 不得出网关日志 */
        var credential = credentialResolver.resolve(appKeyNode.asText());
        byte[] paymentBody = processor.toPaymentCollectBody(encryptedBody, credential);
        JsonNode plain = json.readTree(paymentBody);
        Long merchantId = plain.has("merchantId") ? plain.get("merchantId").asLong() : null;
        String productType = plain.has("productType") && !plain.get("productType").isNull()
                ? plain.get("productType").asText()
                : null;
        String orderNo = plain.has("orderNo") && !plain.get("orderNo").isNull()
                ? plain.get("orderNo").asText()
                : null;
        /* 功能块：Dubbo 收单 — 幂等由 payment 按 orderNo 保证 */
        ApiResponse<?> response = paymentClient.collect(new CollectPaymentCommand(
                merchantId,
                orderNo,
                plain.get("amount").asLong(),
                plain.get("channel").asText(),
                productType));
        if (response.getCode() != 0) {
            throw MerchantSignedCollectException.badRequest(response.getMessage());
        }
        return json.writeValueAsBytes(response.getData());
    }

    private Mono<Void> writeOk(ServerWebExchange exchange, byte[] body) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Gateway-Verified", "merchant-collect");
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    public Mono<Void> writeError(ServerWebExchange exchange, MerchantSignedCollectException ex) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.valueOf(ex.getHttpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":\"" + ex.getCode() + "\",\"message\":\"" + ex.getMessage() + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
