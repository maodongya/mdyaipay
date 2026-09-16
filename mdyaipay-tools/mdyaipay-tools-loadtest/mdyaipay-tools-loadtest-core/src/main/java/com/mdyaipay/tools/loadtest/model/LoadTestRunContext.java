package com.mdyaipay.tools.loadtest.model;

/**
 * 单次 {@link com.mdyaipay.tools.loadtest.spi.LoadTestDriver#execute} 的上下文，供参数化或轮询实例。
 *
 * @param threadIndex 虚拟用户在线程池中的索引 {@code 0 .. threads-1}
 * @param iteration   引擎内全局递增的采样序号（跨线程）
 */
public record LoadTestRunContext(
        int threadIndex,
        long iteration
) {
}
