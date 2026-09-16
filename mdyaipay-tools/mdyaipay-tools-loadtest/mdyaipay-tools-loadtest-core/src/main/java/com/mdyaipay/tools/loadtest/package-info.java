/**
 * 压测核心：场景模型（{@link com.mdyaipay.tools.loadtest.model.LoadTestPlan}）、
 * 引擎编排（{@link com.mdyaipay.tools.loadtest.engine.LoadTestEngine}）、指标与报告。
 * <p>
 * <b>不负责</b>具体协议收发——HTTP/Dubbo/Spring Cloud 见各 {@code mdyaipay-tools-loadtest-*} 驱动模块。
 * <b>不负责</b>分布式多机协调或生产 APM，详见 {@code docs/loadtest-design.md}。
 */
package com.mdyaipay.tools.loadtest;
