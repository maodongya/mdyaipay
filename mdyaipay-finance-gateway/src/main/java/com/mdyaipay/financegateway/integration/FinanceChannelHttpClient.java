package com.mdyaipay.financegateway.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Objects;

/**
 * 经 HTTP 调用 finance-mock 或真实渠道适配服务。
 */
public class FinanceChannelHttpClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Logger LOG = LogManager.getLogger(FinanceChannelHttpClient.class);

    private final String channelBaseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    /**
     * @param channelBaseUrl 下游根 URL
     * @param objectMapper   JSON 序列化
     */
    public FinanceChannelHttpClient(String channelBaseUrl, ObjectMapper objectMapper) {
        this(channelBaseUrl, objectMapper, HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), TIMEOUT);
    }

    /**
     * 测试或自定义 HTTP 客户端入口。
     */
    public FinanceChannelHttpClient(String channelBaseUrl, ObjectMapper objectMapper, HttpClient httpClient) {
        this(channelBaseUrl, objectMapper, httpClient, TIMEOUT);
    }

    /**
     * 自定义请求超时，供单测触发 {@link HttpTimeoutException}。
     */
    public FinanceChannelHttpClient(
            String channelBaseUrl,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            Duration requestTimeout) {
        this.channelBaseUrl = stripTrailingSlash(Objects.requireNonNull(channelBaseUrl));
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.requestTimeout = Objects.requireNonNull(requestTimeout);
    }

    /** 收单 HTTP 转发。 */
    public ApiResponse<ChannelCollectView> collect(ChannelCollectCommand command) {
        return post("/internal/channel/collect", command, new TypeReference<>() { });
    }

    /** 代扣 HTTP 转发。 */
    public ApiResponse<ChannelWithholdView> deduct(ChannelWithholdCommand command) {
        return post("/internal/channel/withhold/deduct", command, new TypeReference<>() { });
    }

    /** 代付 HTTP 转发。 */
    public ApiResponse<ChannelPayoutView> remit(ChannelPayoutCommand command) {
        return post("/internal/channel/payout/remit", command, new TypeReference<>() { });
    }

    private <T> ApiResponse<T> post(String path, Object body, TypeReference<ApiResponse<T>> type) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(channelBaseUrl + path))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOG.error(
                        "{} status={}",
                        AlertEvents.failure(AlertEvents.CHANNEL_HTTP_FAILED),
                        response.statusCode());
                return ApiResponse.fail(
                        ErrorCode.INTERNAL_ERROR.getCode(), "channel http " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), type);
        } catch (HttpTimeoutException ex) {
            LOG.error(AlertEvents.failure(AlertEvents.CHANNEL_HTTP_TIMEOUT), ex);
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage());
        } catch (Exception ex) {
            LOG.error(AlertEvents.failure(AlertEvents.CHANNEL_HTTP_FAILED), ex);
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage());
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
