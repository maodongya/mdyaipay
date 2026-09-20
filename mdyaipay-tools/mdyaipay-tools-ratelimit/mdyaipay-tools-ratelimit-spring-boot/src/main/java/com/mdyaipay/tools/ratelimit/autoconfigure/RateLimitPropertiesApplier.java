package com.mdyaipay.tools.ratelimit.autoconfigure;

import java.util.ArrayList;

/**
 * 将 Nacos 等外部源解析出的 {@link RateLimitProperties} 合并进运行时 Bean（非 null 字段覆盖）。
 */
public final class RateLimitPropertiesApplier {

    private RateLimitPropertiesApplier() {
    }

    /**
     * 合并远程配置到目标 Bean；{@code source} 为 null 时不修改。
     * <p>
     * 幂等：多次合并结果与最后一次 source 中非 null 字段一致。
     *
     * @param target 运行时 {@link RateLimitProperties}
     * @param source 远程快照
     */
    public static void mergeInto(RateLimitProperties target, RateLimitProperties source) {
        if (target == null || source == null) {
            return;
        }
        target.setEnabled(source.isEnabled());
        if (source.getBackend() != null) {
            target.setBackend(source.getBackend());
        }
        target.setFailOpen(source.isFailOpen());
        if (source.getRedis() != null) {
            target.setRedis(source.getRedis());
        }
        if (source.getServlet() != null) {
            target.setServlet(source.getServlet());
        }
        if (source.getDubbo() != null) {
            target.setDubbo(source.getDubbo());
        }
        if (source.getDefaultPolicy() != null) {
            target.setDefaultPolicy(source.getDefaultPolicy());
        }
        if (source.getRules() != null) {
            target.setRules(new ArrayList<>(source.getRules()));
        }
    }
}
