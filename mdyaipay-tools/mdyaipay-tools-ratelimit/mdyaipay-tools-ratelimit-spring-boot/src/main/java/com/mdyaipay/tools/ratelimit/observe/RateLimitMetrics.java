package com.mdyaipay.tools.ratelimit.observe;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * M1 限流观测：单 Counter + 拒绝/后端异常 WARN 日志（无 SPI、无多指标族）。
 * <p>
 * 无 {@link MeterRegistry} 时仅打日志，指标为 no-op。
 */
public final class RateLimitMetrics {

    /** 指标：{@code mdyaipay.ratelimit.decisions}，标签 {@code rule_id}、{@code outcome}、{@code backend}。 */
    public static final String OUTCOME_ALLOWED = "allowed";
    /** 429 拒绝。 */
    public static final String OUTCOME_DENIED = "denied";
    /** Redis/Redisson 异常。 */
    public static final String OUTCOME_BACKEND_ERROR = "backend_error";
    /** 规则命中但 key 为空。 */
    public static final String OUTCOME_SKIPPED = "skipped";

    private static final Logger log = LoggerFactory.getLogger(RateLimitMetrics.class);
    private static final String METRIC_DECISIONS = "mdyaipay.ratelimit.decisions";

    private final MeterRegistry registry;

    /**
     * @param registry Micrometer 注册表，可为 null（仅日志）
     */
    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一次判定并必要时打 WARN 日志。
     * <p>
     * 幂等：是（每次调用独立累加）。
     *
     * @param ruleId         规则 id
     * @param outcome        {@link #OUTCOME_ALLOWED} 等
     * @param backend        配置 backend
     * @param path           请求 path
     * @param limit          策略上限
     * @param durationNanos  tryAcquire 耗时，未调用时为 0
     * @param errorClass     异常简单类名，无则为 null
     * @param failOpenApplied 后端异常且 fail-open 时为 true
     */
    public void record(
            String ruleId,
            String outcome,
            String backend,
            String path,
            long limit,
            long durationNanos,
            String errorClass,
            boolean failOpenApplied) {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(backend, "backend");
        if (registry != null) {
            registry.counter(
                            METRIC_DECISIONS,
                            "rule_id", ruleId,
                            "outcome", outcome,
                            "backend", backend)
                    .increment();
            if (durationNanos > 0L) {
                registry.timer(
                                "mdyaipay.ratelimit.acquire",
                                "rule_id", ruleId,
                                "backend", backend)
                        .record(durationNanos, TimeUnit.NANOSECONDS);
            }
        }
        if (OUTCOME_DENIED.equals(outcome)) {
            log.warn("event=rate_limit_denied rule_id={} path={} limit={} backend={}", ruleId, path, limit, backend);
        } else if (OUTCOME_BACKEND_ERROR.equals(outcome)) {
            log.warn(
                    "event=rate_limit_backend_error rule_id={} path={} fail_open={} exception={} backend={}",
                    ruleId,
                    path,
                    failOpenApplied,
                    errorClass,
                    backend);
        }
        if (!OUTCOME_SKIPPED.equals(outcome)) {
            RateLimitSkyWalkingSupport.tag(ruleId, outcome);
        }
    }
}
