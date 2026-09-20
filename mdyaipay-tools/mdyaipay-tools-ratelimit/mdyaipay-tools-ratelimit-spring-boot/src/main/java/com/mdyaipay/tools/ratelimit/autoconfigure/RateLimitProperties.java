package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 限流配置：{@code mdyaipay.ratelimit.*}。
 * <p>
 * 集群规则可由 Nacos（{@code mdyaipay.ratelimit.nacos}）或 Sentinel {@code cluster:{ruleId}} 动态覆盖。
 */
@ConfigurationProperties(prefix = "mdyaipay.ratelimit")
public class RateLimitProperties {

    /** Exchange attribute：下游可写入已解析的商户 appKey。 */
    public static final String ATTR_MERCHANT_APP_KEY = "mdyaipay.ratelimit.merchantAppKey";

    /** Exchange attribute：命中的限流规则 id（P2 供 Trace 关联）。 */
    public static final String ATTR_RATE_LIMIT_RULE_ID = "mdyaipay.ratelimit.ruleId";

    /** Exchange attribute：限流 outcome（allowed/denied/…）。 */
    public static final String ATTR_RATE_LIMIT_OUTCOME = "mdyaipay.ratelimit.outcome";

    private boolean enabled = true;
    private String backend = "memory";
    private boolean failOpen = false;
    private Redis redis = new Redis();
    private Servlet servlet = new Servlet();
    private Dubbo dubbo = new Dubbo();
    private PolicySpec defaultPolicy;
    private List<RuleSpec> rules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet == null ? new Servlet() : servlet;
    }

    public Dubbo getDubbo() {
        return dubbo;
    }

    public void setDubbo(Dubbo dubbo) {
        this.dubbo = dubbo == null ? new Dubbo() : dubbo;
    }

    public PolicySpec getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(PolicySpec defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public List<RuleSpec> getRules() {
        return rules;
    }

    public void setRules(List<RuleSpec> rules) {
        this.rules = rules == null ? new ArrayList<>() : rules;
    }

    /**
     * Dubbo Provider/Consumer Filter 开关（默认开启，需 classpath 有 Dubbo）。
     */
    public static class Dubbo {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Servlet Filter 开关（默认关闭）。
     */
    public static class Servlet {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Redis 连接配置。
     */
    public static class Redis {
        private String uri = "redis://127.0.0.1:6379";
        private Duration timeout = Duration.ofMillis(200);

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 策略片段（可与 default-policy 合并）。
     */
    public static class PolicySpec {
        private RateLimitAlgorithm algorithm;
        private Long limit;
        private Duration window;
        private Double refillRatePerSecond;
        private Integer slidingSegments;

        public RateLimitAlgorithm getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(RateLimitAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        public Long getLimit() {
            return limit;
        }

        public void setLimit(Long limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public Double getRefillRatePerSecond() {
            return refillRatePerSecond;
        }

        public void setRefillRatePerSecond(Double refillRatePerSecond) {
            this.refillRatePerSecond = refillRatePerSecond;
        }

        public Integer getSlidingSegments() {
            return slidingSegments;
        }

        public void setSlidingSegments(Integer slidingSegments) {
            this.slidingSegments = slidingSegments;
        }
    }

    /**
     * 单条限流规则。
     */
    public static class RuleSpec {
        private String id;
        private MatchSpec match = new MatchSpec();
        private PolicySpec policy;
        private List<String> keyResolvers = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public MatchSpec getMatch() {
            return match;
        }

        public void setMatch(MatchSpec match) {
            this.match = match == null ? new MatchSpec() : match;
        }

        public PolicySpec getPolicy() {
            return policy;
        }

        public void setPolicy(PolicySpec policy) {
            this.policy = policy;
        }

        public List<String> getKeyResolvers() {
            return keyResolvers;
        }

        public void setKeyResolvers(List<String> keyResolvers) {
            this.keyResolvers = keyResolvers == null ? new ArrayList<>() : keyResolvers;
        }
    }

    /**
     * 请求匹配条件。
     */
    public static class MatchSpec {
        private String path;
        private List<String> methods = new ArrayList<>();
        /** Dubbo 侧：{@code provider} 或 {@code consumer}。 */
        private String dubboSide;
        /** Dubbo 接口全限定名。 */
        private String dubboService;
        /** Dubbo 方法名；空表示接口下全部方法。 */
        private String dubboMethod;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods == null ? new ArrayList<>() : methods;
        }

        public String getDubboSide() {
            return dubboSide;
        }

        public void setDubboSide(String dubboSide) {
            this.dubboSide = dubboSide;
        }

        public String getDubboService() {
            return dubboService;
        }

        public void setDubboService(String dubboService) {
            this.dubboService = dubboService;
        }

        public String getDubboMethod() {
            return dubboMethod;
        }

        public void setDubboMethod(String dubboMethod) {
            this.dubboMethod = dubboMethod;
        }
    }
}
