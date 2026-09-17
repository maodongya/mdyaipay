package com.mdyaipay.payment;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付服务 Spring Boot 入口：暴露 Dubbo {@code PaymentGatewayFacade}，无对外 HTTP 业务 API。
 */
@SpringBootApplication
@EnableDubbo
public class MdyaipayPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MdyaipayPaymentApplication.class, args);
    }
}
