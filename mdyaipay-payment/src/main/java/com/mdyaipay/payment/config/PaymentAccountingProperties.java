package com.mdyaipay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付成功后驱动账务的配置：Dubbo（默认）、MQ 或关闭。
 */
@ConfigurationProperties(prefix = "payment.accounting")
public class PaymentAccountingProperties {

    /**
     * 入账通道：{@code dubbo}（默认）、{@code mq}、{@code none}。
     */
    private String channel = "dubbo";

    /** 返回入账通道。 */
    public String getChannel() {
        return channel;
    }

    /** 设置入账通道。 */
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
