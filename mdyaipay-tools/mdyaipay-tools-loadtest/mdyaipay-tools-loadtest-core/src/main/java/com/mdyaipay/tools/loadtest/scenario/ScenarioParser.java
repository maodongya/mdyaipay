package com.mdyaipay.tools.loadtest.scenario;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdyaipay.tools.loadtest.model.LoadProfile;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.MetricsProfile;
import com.mdyaipay.tools.loadtest.model.ReportConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景文件解析：按扩展名选择 YAML 或 JSON，映射为 {@link LoadTestPlan}。
 * <p>
 * 未出现的段使用与设计文档一致的默认值（如 {@link com.mdyaipay.tools.loadtest.model.MetricsProfile#defaults()} 中的分位数需通过 YAML 显式覆盖）。
 * <p>
 * <b>不负责</b> CLI {@code --load.*} 覆盖——见 {@link PlanOverrideApplier}。
 */
public final class ScenarioParser {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());

    /**
     * 从路径读取场景；{@code .yaml/.yml} 用 YAML 解析，否则按 JSON。
     */
    public LoadTestPlan parse(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase();
        ObjectMapper mapper = name.endsWith(".yaml") || name.endsWith(".yml") ? YAML : JSON;
        Map<String, Object> root = mapper.readValue(Files.readString(path), new TypeReference<>() {
        });
        return fromMap(root);
    }

    public LoadTestPlan fromMap(Map<String, Object> root) {
        String planName = string(root, "name", "unnamed");
        String protocol = string(root, "protocol", "http");

        Map<String, Object> loadMap = map(root, "load");
        LoadProfile loadProfile = new LoadProfile(
                intVal(loadMap, "threads", 1),
                longVal(loadMap, "durationSeconds", 60),
                longVal(loadMap, "rampUpSeconds", 0),
                longVal(loadMap, "thinkTimeMillis", 0),
                integerOrNull(loadMap, "targetRps")
        );

        Map<String, Object> metricsMap = map(root, "metrics");
        MetricsProfile metricsProfile = new MetricsProfile(
                doubleList(metricsMap, "percentiles", List.of(0.5, 0.9, 0.95, 0.99)),
                longVal(metricsMap, "warmupSeconds", 10),
                bool(metricsMap, "recordErrors", true),
                intVal(metricsMap, "maxErrorSamples", 100)
        );

        Map<String, Object> target = map(root, "target");
        Map<String, Object> reportMap = map(root, "report");
        ReportConfig reportConfig = new ReportConfig(
                stringList(reportMap, "formats", List.of("console", "json", "markdown")),
                string(reportMap, "outputDir", "target/loadtest-reports")
        );

        return new LoadTestPlan(planName, protocol, loadProfile, metricsProfile, target, reportConfig);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof Map<?, ?> m) {
            return new LinkedHashMap<>((Map<String, Object>) m);
        }
        return new LinkedHashMap<>();
    }

    private static String string(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return v == null ? defaultVal : String.valueOf(v);
    }

    private static int intVal(Map<String, Object> map, String key, int defaultVal) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return defaultVal;
    }

    private static long longVal(Map<String, Object> map, String key, long defaultVal) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        return defaultVal;
    }

    private static Integer integerOrNull(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private static boolean bool(Map<String, Object> map, String key, boolean defaultVal) {
        Object v = map.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        return defaultVal;
    }

    private static List<Double> doubleList(Map<String, Object> map, String key, List<Double> defaultVal) {
        Object v = map.get(key);
        if (!(v instanceof List<?> list)) {
            return defaultVal;
        }
        List<Double> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                out.add(n.doubleValue());
            }
        }
        return out.isEmpty() ? defaultVal : out;
    }

    private static List<String> stringList(Map<String, Object> map, String key, List<String> defaultVal) {
        Object v = map.get(key);
        if (!(v instanceof List<?> list)) {
            return defaultVal;
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            out.add(String.valueOf(o));
        }
        return out.isEmpty() ? defaultVal : out;
    }
}
