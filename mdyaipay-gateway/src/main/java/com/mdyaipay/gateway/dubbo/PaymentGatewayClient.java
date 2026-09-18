package com.mdyaipay.gateway.dubbo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.payment.api.gateway.PaymentGatewayFacade;
import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.payment.api.gateway.dto.PayoutOrderView;
import com.mdyaipay.payment.api.gateway.dto.WithholdOrderView;
import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.tools.trace.http.OutgoingHttpTraceSupport;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 网关 → payment：默认内网 HTTP（本地压测稳定）；未配置 {@code payment-base-url} 时走 Dubbo。
 */
@Component
public class PaymentGatewayClient {

    private final ObjectMapper json;
    private final HttpClient httpClient;
    private final String paymentBaseUrl;

    @DubboReference(
            version = "1.0.0",
            check = false,
            protocol = "dubbo",
            url = "${mdyaipay.gateway.payment-dubbo-url:}")
    private PaymentGatewayFacade paymentGatewayFacade;

    public PaymentGatewayClient(
            ObjectMapper json,
            @Value("${mdyaipay.gateway.payment-base-url:}") String paymentBaseUrl) {
        this.json = json;
        this.paymentBaseUrl = stripTrailingSlash(paymentBaseUrl);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public ApiResponse<PaymentOrderView> collect(CollectPaymentCommand command) {
        if (useHttp()) {
            return postCollect(command);
        }
        return paymentGatewayFacade.collect(command);
    }

    public ApiResponse<PaymentOrderView> getPayment(String orderNo) {
        return paymentGatewayFacade.getPayment(orderNo);
    }

    public ApiResponse<PaymentOrderView> confirmChannelPayment(ChannelConfirmCommand command) {
        return paymentGatewayFacade.confirmChannelPayment(command);
    }

    public ApiResponse<WithholdOrderView> createWithhold(CreateWithholdCommand command) {
        return paymentGatewayFacade.createWithhold(command);
    }

    public ApiResponse<PayoutOrderView> createPayout(CreatePayoutCommand command) {
        return paymentGatewayFacade.createPayout(command);
    }

    private boolean useHttp() {
        return paymentBaseUrl != null && !paymentBaseUrl.isBlank();
    }

    private ApiResponse<PaymentOrderView> postCollect(CollectPaymentCommand command) {
        try {
            String body = json.writeValueAsString(command);
            HttpRequest.Builder builder = HttpRequest.newBuilder(
                            URI.create(paymentBaseUrl + "/internal/v1/payment-gateway/collect"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            OutgoingHttpTraceSupport.inject(builder);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return ApiResponse.fail(response.statusCode(), "payment http " + response.statusCode());
            }
            JsonNode root = json.readTree(response.body());
            int code = root.path("code").asInt(-1);
            String message = root.path("message").asText("");
            if (code != 0) {
                return ApiResponse.fail(code, message);
            }
            PaymentOrderView data = json.treeToValue(root.get("data"), PaymentOrderView.class);
            return ApiResponse.ok(data);
        } catch (Exception ex) {
            return ApiResponse.fail(500, ex.getMessage());
        }
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
