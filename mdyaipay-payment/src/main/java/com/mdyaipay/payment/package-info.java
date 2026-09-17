/**
 * 支付核心模块（Spring Boot + Dubbo Provider，HTTP 仅 actuator；业务入口见 {@code mdyaipay-payment-api}）。
 * <p>
 * 分层：{@code api.dubbo} / {@code service} / {@code domain} / {@code repository} / {@code mybatis}；
 * 渠道适配占位见 {@code gateway}（Mock 实现）。
 */
package com.mdyaipay.payment;
