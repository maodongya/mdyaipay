package com.mdyaipay.financegateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 渠道网关配置：下游 HTTP 基址（finance-mock 或银行联调地址）。
 */
@ConfigurationProperties(prefix = "finance-gateway")
public class FinanceGatewayProperties {

    /** 下游渠道 HTTP 根 URL，不含尾斜杠。 */
    private String channelBaseUrl = "http://127.0.0.1:8097";

    public String getChannelBaseUrl() {
        return channelBaseUrl;
    }

    public void setChannelBaseUrl(String channelBaseUrl) {
        this.channelBaseUrl = channelBaseUrl;
    }
}
