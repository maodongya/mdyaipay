package com.mdyaipay.tools.loadtest.dubbo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriver;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo 泛化调用驱动：{@link GenericService#$invoke}，避免依赖业务接口 jar。
 * <p>
 * {@code target.url} 直连时跳过注册中心；否则需 {@code registryAddress}。
 * 默认 {@code retries=0}，避免压测时重复放大流量。
 */
public final class DubboLoadTestDriver implements LoadTestDriver {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final ConcurrentHashMap<String, GenericService> services = new ConcurrentHashMap<>();

    @Override
    public String protocol() {
        return "dubbo";
    }

    @Override
    public SampleOutcome execute(LoadTestPlan plan, LoadTestRunContext runContext) {
        Map<String, Object> target = plan.target();
        long start = System.nanoTime();
        try {
            /* 功能块：复用 Reference — 与生产客户端类似的长连接/连接池行为 */
            GenericService service = services.computeIfAbsent(cacheKey(target), k -> createService(target));
            String method = string(target, "method");
            String[] parameterTypes = stringArray(target, "parameterTypes");
            Object[] args = parseArgs(target.get("argsJson"));
            service.$invoke(method, parameterTypes, args);
            long latency = System.nanoTime() - start;
            return new SampleOutcome(true, latency, 0, null);
        } catch (Exception e) {
            long latency = System.nanoTime() - start;
            return new SampleOutcome(false, latency, -1, e.getMessage());
        }
    }

    private static GenericService createService(Map<String, Object> target) {
        ApplicationConfig app = new ApplicationConfig("mdyaipay-loadtest");
        ReferenceConfig<GenericService> ref = new ReferenceConfig<>();
        ref.setApplication(app);
        ref.setInterface(string(target, "interfaceName"));
        ref.setGeneric(true);
        ref.setRetries(intVal(target, "retries", 0));
        ref.setTimeout(intVal(target, "timeoutMillis", 5000));
        if (target.containsKey("group")) {
            ref.setGroup(string(target, "group"));
        }
        if (target.containsKey("version")) {
            ref.setVersion(string(target, "version"));
        }
        if (target.containsKey("url")) {
            ref.setUrl(string(target, "url"));
        } else {
            RegistryConfig registry = new RegistryConfig(string(target, "registryAddress"));
            ref.setRegistry(registry);
        }
        return ref.get();
    }

    private static Object[] parseArgs(Object raw) throws Exception {
        if (raw == null) {
            return new Object[0];
        }
        if (raw instanceof String s) {
            List<Object> list = JSON.readValue(s, new TypeReference<>() {
            });
            return list.toArray();
        }
        if (raw instanceof List<?> list) {
            return list.toArray();
        }
        throw new IllegalArgumentException("argsJson must be JSON array string or YAML list");
    }

    private static String cacheKey(Map<String, Object> target) {
        return string(target, "interfaceName") + "|" + target.getOrDefault("url", target.get("registryAddress"));
    }

    private static String string(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            throw new IllegalArgumentException("target." + key + " is required");
        }
        return String.valueOf(v);
    }

    private static int intVal(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private static String[] stringArray(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof List<?> list)) {
            return new String[0];
        }
        return list.stream().map(String::valueOf).toArray(String[]::new);
    }
}
