package com.mdyaipay.accounting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 账务 Spring 上下文加载冒烟测试。
 */
@SpringBootTest
class MdyaipayAccountingApplicationTests {

    /** 验证 Bean 装配无循环依赖。 */
    @Test
    void contextLoads() {
    }
}
