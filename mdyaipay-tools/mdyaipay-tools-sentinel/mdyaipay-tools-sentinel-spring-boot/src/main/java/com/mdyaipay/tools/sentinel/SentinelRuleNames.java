package com.mdyaipay.tools.sentinel;

/**
 * Sentinel 与 Dashboard 共用的资源名前缀约定。
 */
public final class SentinelRuleNames {

    /** Dashboard 流控资源前缀：同步到 Redis/Redisson 整体限流，不在本机 Sentinel 扣减。 */
    public static final String CLUSTER_PREFIX = "cluster:";

    /** 本机 Sentinel 流控资源前缀。 */
    public static final String LOCAL_PREFIX = "local:";

    private SentinelRuleNames() {
    }

    /**
     * 构造本机限流资源名。
     *
     * @param ruleId mdyaipay 规则 id
     * @return {@code local:{ruleId}}
     */
    public static String localResource(String ruleId) {
        return LOCAL_PREFIX + ruleId;
    }

    /**
     * 从 Dashboard 资源名解析集群 ruleId。
     *
     * @param resource Sentinel 资源名
     * @return ruleId；非 cluster 前缀时 empty
     */
    public static java.util.Optional<String> clusterRuleId(String resource) {
        if (resource == null || !resource.startsWith(CLUSTER_PREFIX)) {
            return java.util.Optional.empty();
        }
        String id = resource.substring(CLUSTER_PREFIX.length()).trim();
        if (id.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(id);
    }
}
