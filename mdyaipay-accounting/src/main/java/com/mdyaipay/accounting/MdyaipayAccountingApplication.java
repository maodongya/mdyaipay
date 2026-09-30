package com.mdyaipay.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 账务域 Spring Boot 入口：默认 HTTP {@code 8083}，Dubbo Provider {@code 20883}。
 */
@SpringBootApplication
public class MdyaipayAccountingApplication {

    /**
     * 启动账务服务进程。
     */
    public static void main(String[] args) {
        SpringApplication.run(MdyaipayAccountingApplication.class, args);
    }
}
