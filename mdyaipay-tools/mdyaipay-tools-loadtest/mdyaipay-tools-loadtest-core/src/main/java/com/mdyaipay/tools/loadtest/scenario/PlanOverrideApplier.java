package com.mdyaipay.tools.loadtest.scenario;

import com.mdyaipay.tools.loadtest.model.LoadProfile;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.MetricsProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 命令行参数覆盖：在 {@link ScenarioParser} 结果之上用 {@code --段.字段=值} 调整负载与指标。
 * <p>
 * 覆盖仅影响内存中的 {@link LoadTestPlan} 副本，不回写场景文件。
 */
public final class PlanOverrideApplier {

    /**
     * 顺序应用多条覆盖；无法识别的键静默忽略。
     */
    public LoadTestPlan apply(LoadTestPlan plan, List<String> overrides) {
        LoadProfile load = plan.loadProfile();
        MetricsProfile metrics = plan.metricsProfile();
        String planName = plan.name();
        for (String override : overrides) {
            if (!override.startsWith("--") || !override.contains("=")) {
                continue;
            }
            String body = override.substring(2);
            int eq = body.indexOf('=');
            String key = body.substring(0, eq);
            String value = body.substring(eq + 1);
            switch (key) {
                case "load.threads" -> load = new LoadProfile(
                        Integer.parseInt(value), load.durationSeconds(), load.rampUpSeconds(),
                        load.thinkTimeMillis(), load.targetRps());
                case "load.durationSeconds" -> load = new LoadProfile(
                        load.threads(), Long.parseLong(value), load.rampUpSeconds(),
                        load.thinkTimeMillis(), load.targetRps());
                case "load.rampUpSeconds" -> load = new LoadProfile(
                        load.threads(), load.durationSeconds(), Long.parseLong(value),
                        load.thinkTimeMillis(), load.targetRps());
                case "load.thinkTimeMillis" -> load = new LoadProfile(
                        load.threads(), load.durationSeconds(), load.rampUpSeconds(),
                        Long.parseLong(value), load.targetRps());
                case "load.targetRps" -> load = new LoadProfile(
                        load.threads(), load.durationSeconds(), load.rampUpSeconds(),
                        load.thinkTimeMillis(), value.isBlank() ? null : Integer.parseInt(value));
                case "metrics.warmupSeconds" -> metrics = new MetricsProfile(
                        metrics.percentiles(), Long.parseLong(value), metrics.recordErrors(),
                        metrics.maxErrorSamples());
                case "metrics.maxErrorSamples" -> metrics = new MetricsProfile(
                        metrics.percentiles(), metrics.warmupSeconds(), metrics.recordErrors(),
                        Integer.parseInt(value));
                case "metrics.recordErrors" -> metrics = new MetricsProfile(
                        metrics.percentiles(), metrics.warmupSeconds(), Boolean.parseBoolean(value),
                        metrics.maxErrorSamples());
                case "metrics.percentiles" -> {
                    List<Double> ps = new ArrayList<>();
                    for (String p : value.split(",")) {
                        ps.add(Double.parseDouble(p.trim()));
                    }
                    metrics = new MetricsProfile(ps, metrics.warmupSeconds(), metrics.recordErrors(),
                            metrics.maxErrorSamples());
                }
                case "name" -> planName = value;
                default -> {
                }
            }
        }
        return new LoadTestPlan(planName, plan.protocol(), load, metrics, plan.target(), plan.reportConfig());
    }

    public static List<String> filterOverrides(String[] args) {
        return Arrays.stream(args)
                .filter(a -> a.startsWith("--") && a.contains("="))
                .toList();
    }

    public static String scenarioPath(String[] args) {
        return Arrays.stream(args)
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .orElse(null);
    }
}
