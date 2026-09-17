package com.mdyaipay.gateway;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 对外 Spring Cloud Gateway 入口：商户加密收单、支付/代扣/代付经 Dubbo 调 user/payment（ZK 注册发现）。
 */
@SpringBootApplication
@EnableDubbo
public class MdyaipayGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MdyaipayGatewayApplication.class, args);
    }
}