package com.mdyaipay.tools.ratelimit.observe;

/**
 * SkyWalking 入口 Span 自定义 Tag（反射调用 ActiveSpan，无 toolkit 依赖）。
 * <p>
 * <b>不负责</b> Span 创建——依赖 Java Agent 已挂载。
 */
final class RateLimitSkyWalkingSupport {

    private static final String ACTIVE_SPAN = "org.apache.skywalking.apm.toolkit.trace.ActiveSpan";
    private static final boolean AVAILABLE = probe();

    private RateLimitSkyWalkingSupport() {
    }

    /**
     * 探测 classpath 是否存在 SkyWalking toolkit。
     */
    private static boolean probe() {
        try {
            Class.forName(ACTIVE_SPAN);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * 为当前活跃 Span 写入限流 Tag；Agent 未挂载或无 toolkit 时为 no-op。
     *
     * @param ruleId  规则 id
     * @param outcome {@link RateLimitMetrics} outcome 常量
     */
    static void tag(String ruleId, String outcome) {
        if (!AVAILABLE || ruleId == null || outcome == null) {
            return;
        }
        try {
            Class<?> activeSpan = Class.forName(ACTIVE_SPAN);
            activeSpan.getMethod("tag", String.class, String.class).invoke(null, "ratelimit.rule_id", ruleId);
            activeSpan.getMethod("tag", String.class, String.class).invoke(null, "ratelimit.outcome", outcome);
        } catch (ReflectiveOperationException ignored) {
            // Agent 未激活或 API 不可用
        }
    }
}
