package com.mdyaipay.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 财务域 Spring Boot 入口：默认 HTTP {@code 8084}，Dubbo Provider {@code 20884}。
 */
@SpringBootApplication
public class MdyaipayFinanceApplication {

    /**
     * 启动财务服务进程。
     */
    public static void main(String[] args) {
        SpringApplication.run(MdyaipayFinanceApplication.class, args);
    }
}
