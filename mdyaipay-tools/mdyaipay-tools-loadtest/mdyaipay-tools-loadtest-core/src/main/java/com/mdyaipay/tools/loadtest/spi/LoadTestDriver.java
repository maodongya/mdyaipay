package com.mdyaipay.tools.loadtest.spi;

import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;

/**
 * 压测协议驱动 SPI：执行<strong>一次</strong>采样（一次 HTTP 请求或一次 RPC 调用）。
 * <p>
 * 由 {@link com.mdyaipay.tools.loadtest.engine.LoadTestEngine} 按 {@link com.mdyaipay.tools.loadtest.model.LoadProfile}
 * 并发调度；实现类通过 {@code META-INF/services} 注册，由 {@link LoadTestDriverRegistry} 发现。
 * <p>
 * <b>不负责</b>：线程池、Ramp-up、预热、指标汇总——这些在 core 引擎内完成。
 */
public interface LoadTestDriver {

    /**
     * 协议标识，与场景文件 {@code protocol} 一致（小写），如 {@code http}、{@code dubbo}、{@code springcloud}。
     */
    String protocol();

    /**
     * 执行单次采样。
     *
     * @param plan       完整场景（含 {@code target} 协议参数）
     * @param runContext 当前虚拟用户线程索引与全局迭代序号，可用于参数化或轮询
     * @return 耗时（纳秒）、HTTP/RPC 成败与状态码
     * @throws Exception 未捕获时引擎记为失败样本，不中断整轮压测
     */
    SampleOutcome execute(LoadTestPlan plan, LoadTestRunContext runContext) throws Exception;
}
