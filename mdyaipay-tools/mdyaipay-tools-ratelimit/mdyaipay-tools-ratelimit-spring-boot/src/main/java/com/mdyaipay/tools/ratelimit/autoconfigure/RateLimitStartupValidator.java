package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboConsumerFilter;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboProviderFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 启动时校验限流装配：避免 {@code enabled=true} 却无 {@link RateLimiter} 时 Filter 静默缺失。
 */
final class RateLimitStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RateLimitStartupValidator.class);

    /** {@link RateLimitGatewayAutoConfiguration} 注册的 Bean 名；勿注入 Filter 类型以免在非 Gateway 应用加载 GlobalFilter。 */
    private static final String GATEWAY_FILTER_BEAN = "rateLimitGatewayFilter";

    private final RateLimitProperties properties;
    private final ConfigurableApplicationContext applicationContext;
    private final ObjectProvider<RateLimiter> rateLimiter;
    private final ObjectProvider<RateLimitDubboProviderFilter> dubboProviderFilter;
    private final ObjectProvider<RateLimitDubboConsumerFilter> dubboConsumerFilter;

    /**
     * @param properties           限流配置
     * @param applicationContext   Spring 上下文（按 Bean 名探测 Gateway Filter，避免强依赖 Gateway 类）
     * @param rateLimiter          后端限流器（可选）
     * @param dubboProviderFilter  Dubbo Provider Filter（可选）
     * @param dubboConsumerFilter  Dubbo Consumer Filter（可选）
     */
    RateLimitStartupValidator(
            RateLimitProperties properties,
            ConfigurableApplicationContext applicationContext,
            ObjectProvider<RateLimiter> rateLimiter,
            ObjectProvider<RateLimitDubboProviderFilter> dubboProviderFilter,
            ObjectProvider<RateLimitDubboConsumerFilter> dubboConsumerFilter) {
        this.properties = properties;
        this.applicationContext = applicationContext;
        this.rateLimiter = rateLimiter;
        this.dubboProviderFilter = dubboProviderFilter;
        this.dubboConsumerFilter = dubboConsumerFilter;
    }

    /**
     * 校验 backend、规则与 Bean 是否一致。
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("mdyaipay.ratelimit 已关闭");
            return;
        }
        RateLimiter limiter = rateLimiter.getIfAvailable();
        if (limiter == null) {
            throw new IllegalStateException(
                    "mdyaipay.ratelimit.enabled=true 但未装配 RateLimiter；"
                            + "请检查 mdyaipay.ratelimit.backend="
                            + properties.getBackend()
                            + " 与 classpath（redisson 需 mdyaipay-tools-ratelimit-redisson，"
                            + "redis 需 mdyaipay-tools-ratelimit-redis），并执行 mvn install");
        }
        int ruleCount = properties.getRules() == null ? 0 : properties.getRules().size();
        if (ruleCount == 0) {
            log.warn("mdyaipay.ratelimit 已启用但 rules 为空，不会拦截任何请求");
        }
        boolean hasHttpRules = hasHttpRules();
        boolean hasDubboRules = hasDubboRules();
        boolean gatewayRegistered = applicationContext.containsBean(GATEWAY_FILTER_BEAN);
        log.info(
                "mdyaipay.ratelimit 已启用 backend={} rules={} rateLimiter={} gatewayFilter={} dubboProvider={} dubboConsumer={}",
                properties.getBackend(),
                ruleCount,
                limiter.getClass().getSimpleName(),
                gatewayRegistered ? "registered" : "absent",
                dubboProviderFilter.getIfAvailable() != null ? "registered" : "absent",
                dubboConsumerFilter.getIfAvailable() != null ? "registered" : "absent");
        if (hasHttpRules && !gatewayRegistered) {
            throw new IllegalStateException(
                    "mdyaipay.ratelimit 含 HTTP 规则但未注册 RateLimitGatewayFilter；"
                            + "请确认依赖 spring-cloud-starter-gateway 且存在 ApiResponse（common-core）");
        }
        if (hasDubboRules && properties.getDubbo().isEnabled()
                && dubboProviderFilter.getIfAvailable() == null
                && dubboConsumerFilter.getIfAvailable() == null) {
            throw new IllegalStateException(
                    "mdyaipay.ratelimit 含 Dubbo 规则但未注册 Dubbo Filter；请确认 classpath 有 dubbo-spring-boot-starter");
        }
    }

    private boolean hasHttpRules() {
        if (properties.getRules() == null) {
            return false;
        }
        for (RateLimitProperties.RuleSpec rule : properties.getRules()) {
            if (rule != null && rule.getMatch() != null) {
                String path = rule.getMatch().getPath();
                if (path != null && !path.isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDubboRules() {
        if (properties.getRules() == null) {
            return false;
        }
        for (RateLimitProperties.RuleSpec rule : properties.getRules()) {
            if (rule != null && rule.getMatch() != null) {
                String side = rule.getMatch().getDubboSide();
                if (side != null && !side.isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }
}
