package com.mdyaipay.tools.loadtest.springcloud;

import com.mdyaipay.tools.loadtest.http.HttpRequestExecutor;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriver;
import feign.Client;
import feign.Request;
import feign.Response;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spring Cloud 场景驱动：将 {@code target.mode} 解析为最终 URL 后走 HTTP 或 Feign {@link Client}。
 * <p>
 * {@code loadbalancer} 依赖静态 {@code instances} 列表（简单发现），按 {@link com.mdyaipay.tools.loadtest.model.LoadTestRunContext#iteration()} 轮询。
 */
public final class SpringCloudLoadTestDriver implements LoadTestDriver {

    private final HttpRequestExecutor httpExecutor = new HttpRequestExecutor();
    private final Client feignClient = new Client.Default(null, null);

    @Override
    public String protocol() {
        return "springcloud";
    }

    @Override
    public SampleOutcome execute(LoadTestPlan plan, LoadTestRunContext runContext) throws Exception {
        Map<String, Object> target = plan.target();
        String mode = stringOrDefault(target, "mode", "direct-url").toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "feign" -> executeFeign(target, runContext);
            case "loadbalancer", "direct-url" -> httpExecutor.execute(resolvedHttpTarget(target, runContext), runContext);
            default -> throw new IllegalArgumentException("Unsupported springcloud mode: " + mode);
        };
    }

    private SampleOutcome executeFeign(Map<String, Object> target, LoadTestRunContext ctx) throws Exception {
        Map<String, Object> httpTarget = resolvedHttpTarget(target, ctx);
        long start = System.nanoTime();
        String url = String.valueOf(httpTarget.get("url"));
        String method = stringOrDefault(httpTarget, "method", "GET");
        long timeoutMs = httpTarget.get("timeoutMillis") instanceof Number n ? n.longValue() : 5000L;
        Map<String, Collection<String>> headers = feignHeaders(httpTarget.get("headers"));
        byte[] body = httpTarget.get("body") == null ? null : String.valueOf(httpTarget.get("body")).getBytes(StandardCharsets.UTF_8);
        Request.HttpMethod httpMethod = Request.HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        Request.Body reqBody = body == null ? Request.Body.empty() : Request.Body.create(body);
        Request request = Request.create(httpMethod, url, headers, reqBody, null);
        int timeout = (int) Math.min(Integer.MAX_VALUE, timeoutMs);
        Request.Options options = new Request.Options(timeout, timeout);
        try (Response response = feignClient.execute(request, options)) {
            long latency = System.nanoTime() - start;
            int status = response.status();
            boolean ok = expectStatus(httpTarget).contains(status);
            return new SampleOutcome(ok, latency, status, ok ? null : "unexpected status " + status);
        }
    }

    /**
     * 将 serviceId/path/instances 解析为带 {@code url} 的 HTTP target 副本，供 HttpClient 或 Feign 使用。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolvedHttpTarget(Map<String, Object> target, LoadTestRunContext ctx) {
        Map<String, Object> copy = new HashMap<>(target);
        String mode = stringOrDefault(target, "mode", "direct-url").toLowerCase(Locale.ROOT);
        if ("direct-url".equals(mode) && target.containsKey("url")) {
            return copy;
        }
        String path = stringOrDefault(target, "path", "/");
        List<String> instances = instanceList(target);
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("target.instances or target.url required for springcloud");
        }
        String base = instances.get((int) (ctx.iteration() % instances.size()));
        if (base.endsWith("/") && path.startsWith("/")) {
            copy.put("url", base.substring(0, base.length() - 1) + path);
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            copy.put("url", base + "/" + path);
        } else {
            copy.put("url", base + path);
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static List<String> instanceList(Map<String, Object> target) {
        Object v = target.get("instances");
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        if (target.get("url") != null) {
            return List.of(String.valueOf(target.get("url")));
        }
        String serviceId = stringOrDefault(target, "serviceId", "service");
        String discovery = stringOrDefault(target, "discovery", "simple");
        if ("simple".equals(discovery)) {
            throw new IllegalArgumentException("simple discovery requires target.instances for service " + serviceId);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Collection<String>> feignHeaders(Object raw) {
        Map<String, Collection<String>> out = new HashMap<>();
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> out.put(String.valueOf(k), List.of(String.valueOf(v))));
        }
        return out;
    }

    private static List<Integer> expectStatus(Map<String, Object> target) {
        Object v = target.get("expectStatus");
        if (!(v instanceof List<?> list)) {
            return List.of(200);
        }
        List<Integer> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                out.add(n.intValue());
            }
        }
        return out.isEmpty() ? List.of(200) : out;
    }

    private static String stringOrDefault(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v == null ? def : String.valueOf(v);
    }
}
