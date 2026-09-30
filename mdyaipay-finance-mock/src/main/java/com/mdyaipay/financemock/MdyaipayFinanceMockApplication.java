package com.mdyaipay.financemock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模拟渠道 HTTP 服务入口，默认端口 {@code 8097}。
 */
@SpringBootApplication
public class MdyaipayFinanceMockApplication {

    /** 启动 mock 渠道进程。 */
    public static void main(String[] args) {
        SpringApplication.run(MdyaipayFinanceMockApplication.class, args);
    }
}
