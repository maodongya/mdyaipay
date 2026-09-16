package com.mdyaipay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Cloud Gateway entry point for external traffic routing. */
@SpringBootApplication
public class MdyaipayGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MdyaipayGatewayApplication.class, args);
    }
}