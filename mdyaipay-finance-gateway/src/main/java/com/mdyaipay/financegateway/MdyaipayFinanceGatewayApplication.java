package com.mdyaipay.financegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 渠道网关 Spring Boot 入口：Dubbo {@code 20887}，HTTP 转发下游。
 */
@SpringBootApplication
public class MdyaipayFinanceGatewayApplication {

    /** 启动 finance-gateway 进程。 */
    public static void main(String[] args) {
        SpringApplication.run(MdyaipayFinanceGatewayApplication.class, args);
    }
}
