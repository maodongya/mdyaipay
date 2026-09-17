package com.mdyaipay.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 用户域 Spring Boot 入口：默认 HTTP {@code 8082}，Dubbo Provider {@code 20882}。
 */
@SpringBootApplication
@EnableScheduling
public class MdyaipayUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MdyaipayUserApplication.class, args);
    }
}
