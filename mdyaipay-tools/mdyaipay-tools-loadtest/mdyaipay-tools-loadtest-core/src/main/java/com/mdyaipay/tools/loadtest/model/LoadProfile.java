package com.mdyaipay.tools.loadtest.model;

/**
 * 负载模型，对应场景文件 {@code load} 段；由 {@link com.mdyaipay.tools.loadtest.engine.LoadTestEngine} 解释。
 *
 * @param threads          并发虚拟用户数（固定大小线程池）
 * @param durationSeconds  正式阶段最长运行时间（秒）；预热时长见 {@link MetricsProfile#warmupSeconds()}
 * @param rampUpSeconds    线性 Ramp-up：各线程错开启动的总窗口（秒），{@code 0} 表示同时开满
 * @param thinkTimeMillis  每次<strong>成功</strong>采样后休眠（毫秒），模拟用户间隔
 * @param targetRps        非空且 {@code >0} 时启用全局限速（令牌间隔），与 {@code threads} 共同封顶吞吐
 */
public record LoadProfile(
        int threads,
        long durationSeconds,
        long rampUpSeconds,
        long thinkTimeMillis,
        Integer targetRps
) {
}
