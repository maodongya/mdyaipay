/**
 * 支付核心模块（Spring Boot + Dubbo Provider，HTTP 仅 actuator；业务入口见 {@code mdyaipay-payment-api}）。
 * <p>
 * 分层：{@code api} / {@code service} / {@code domain}（聚合与枚举） /
 * {@code repository}（仓储接口）+ {@code repository.mybatis}（实现） /
 * {@code gateway}（渠道接口）+ {@code gateway.mock}（Mock）。
 */
package com.mdyaipay.payment;
