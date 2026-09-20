package com.mdyaipay.tools.sentinel.dubbo;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.match.RateLimitDubboRuleMatcher;
import com.mdyaipay.tools.sentinel.SentinelRuleNames;
import com.mdyaipay.tools.sentinel.autoconfigure.MdyaipaySentinelProperties;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.Optional;

/**
 * Dubbo 本机 Sentinel 限流：在 Redis 整体限流 Filter 之前 {@link SphU#entry}。
 */
public final class SentinelLocalDubboFilterSupport {

    /** 与 {@link com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboFilterSupport} 对齐的 RPC 429。 */
    static final int RPC_RATE_LIMITED = 429;

    private final MdyaipaySentinelProperties sentinelProperties;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitDubboRuleMatcher ruleMatcher;

    /**
     * @param sentinelProperties Sentinel 配置
     * @param rateLimitProperties 规则来源
     * @param ruleMatcher Dubbo 规则匹配
     */
    public SentinelLocalDubboFilterSupport(
            MdyaipaySentinelProperties sentinelProperties,
            RateLimitProperties rateLimitProperties,
            RateLimitDubboRuleMatcher ruleMatcher) {
        this.sentinelProperties = sentinelProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.ruleMatcher = ruleMatcher;
    }

    /**
     * 本机 Sentinel 限流；拒绝时抛 {@link RpcException}（429）。
     *
     * @param side provider 或 consumer
     * @param invoker 调用目标
     * @param invocation 调用参数
     */
    void entryOrThrow(String side, Invoker<?> invoker, Invocation invocation) {
        if (!sentinelProperties.isEnabled() || !rateLimitProperties.isEnabled()) {
            return;
        }
        String service = invoker.getInterface().getName();
        String method = invocation.getMethodName();
        Optional<RateLimitProperties.RuleSpec> matched =
                ruleMatcher.firstMatch(rateLimitProperties.getRules(), side, service, method);
        if (matched.isEmpty()) {
            return;
        }
        String ruleId = matched.get().getId();
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        String resource = SentinelRuleNames.localResource(ruleId);
        try (Entry ignored = SphU.entry(resource)) {
            // 允许：由 try-with-resources 自动 exit
        } catch (BlockException ex) {
            throw new RpcException(RPC_RATE_LIMITED, "Sentinel local rate limited: " + resource);
        }
    }
}
