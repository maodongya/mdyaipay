package com.mdyaipay.tools.loadtest.model;

/**
 * 单次采样结果，由 {@link com.mdyaipay.tools.loadtest.spi.LoadTestDriver} 返回。
 *
 * @param success       是否判定为成功（HTTP 状态在 expect 列表内、RPC 无业务异常等）
 * @param latencyNanos  端到端 wall time（纳秒），失败样本也应尽量填实际耗时
 * @param statusCode    HTTP 状态码；非 HTTP 协议可用 {@code 0} 成功、负值表示失败
 * @param errorMessage  失败摘要；成功时为 {@code null}
 */
public record SampleOutcome(
        boolean success,
        long latencyNanos,
        int statusCode,
        String errorMessage
) {
}
