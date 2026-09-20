package com.mdyaipay.tools.sentinel.autoconfigure;

import com.alibaba.csp.sentinel.property.PropertyListener;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitClusterPolicyRefresher;
import com.mdyaipay.tools.sentinel.SentinelRuleNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 Dashboard 下发的 {@code cluster:} 流控规则同步到整体限流配置，并避免 Sentinel 在本机执行 cluster 规则。
 */
public final class SentinelClusterFlowRuleBridge {

    private static final Logger log = LoggerFactory.getLogger(SentinelClusterFlowRuleBridge.class);

    private final RateLimitClusterPolicyRefresher clusterRefresher;
    private volatile boolean applying;

    /**
     * @param clusterRefresher 集群 limit 刷新器
     */
    public SentinelClusterFlowRuleBridge(RateLimitClusterPolicyRefresher clusterRefresher) {
        this.clusterRefresher = clusterRefresher;
    }

    /**
     * 挂载 SentinelProperty 监听（Dashboard 推送时触发）。
     */
    void registerFlowPropertyListener() {
        try {
            Field field = FlowRuleManager.class.getDeclaredField("currentProperty");
            field.setAccessible(true);
            Object raw = field.get(null);
            if (raw instanceof SentinelProperty<?> property) {
                @SuppressWarnings("unchecked")
                SentinelProperty<List<FlowRule>> typed = (SentinelProperty<List<FlowRule>>) property;
                typed.addListener(new PropertyListener<List<FlowRule>>() {
                    @Override
                    public void configUpdate(List<FlowRule> value) {
                        onFlowRulesChanged(value);
                    }

                    @Override
                    public void configLoad(List<FlowRule> value) {
                        onFlowRulesChanged(value);
                    }
                });
                log.info("event=sentinel_cluster_bridge listener=registered");
            }
        } catch (ReflectiveOperationException ex) {
            log.warn("event=sentinel_cluster_bridge listener=fallback_poll reason={}", ex.toString());
        }
        onFlowRulesChanged(FlowRuleManager.getRules());
    }

    /**
     * 兜底：周期同步 Dashboard 已下发的规则（无 Property 监听时）。
     */
    @Scheduled(fixedDelayString = "${mdyaipay.sentinel.cluster-sync-ms:3000}")
    void pollFlowRules() {
        onFlowRulesChanged(FlowRuleManager.getRules());
    }

    /**
     * 拆分 cluster / local 规则并刷新 Redis 侧 limit。
     *
     * @param rules 当前全部流控规则
     */
    void onFlowRulesChanged(List<FlowRule> rules) {
        if (applying) {
            return;
        }
        applying = true;
        try {
            List<FlowRule> localOnly = new ArrayList<>();
            if (rules != null) {
                for (FlowRule rule : rules) {
                    if (rule == null || rule.getResource() == null) {
                        continue;
                    }
                    String resource = rule.getResource();
                    if (resource.startsWith(SentinelRuleNames.CLUSTER_PREFIX)) {
                        SentinelRuleNames.clusterRuleId(resource)
                                .ifPresent(id -> clusterRefresher.updateLimit(id, (long) rule.getCount()));
                        continue;
                    }
                    localOnly.add(rule);
                }
            }
            List<FlowRule> current = FlowRuleManager.getRules();
            if (!sameRules(localOnly, current)) {
                FlowRuleManager.loadRules(localOnly);
            }
        } finally {
            applying = false;
        }
    }

    private static boolean sameRules(List<FlowRule> a, List<FlowRule> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            FlowRule left = a.get(i);
            FlowRule right = b.get(i);
            if (left == null || right == null) {
                return false;
            }
            if (!left.getResource().equals(right.getResource())) {
                return false;
            }
            if (Double.compare(left.getCount(), right.getCount()) != 0) {
                return false;
            }
        }
        return true;
    }
}
