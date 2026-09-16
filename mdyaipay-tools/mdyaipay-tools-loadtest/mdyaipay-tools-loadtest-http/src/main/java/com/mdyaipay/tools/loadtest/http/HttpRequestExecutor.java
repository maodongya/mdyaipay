package com.mdyaipay.tools.loadtest.http;

import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 单次 HTTP 采样：读 {@code target} 中的 url/method/headers/body，判定 {@code expectStatus}。
 * <p>
 * 响应体丢弃（{@code BodyHandlers.discarding()}），降低压测客户端内存占用。
 */
public final class HttpRequestExecutor {

    public SampleOutcome execute(Map<String, Object> target, LoadTestRunContext runContext) throws Exception {
        long start = System.nanoTime();
        HttpClient client = HttpClientFactory.clientFor(target);
        String url = templatize(HttpTargetSupport.url(target), runContext);
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(HttpTargetSupport.toUri(url))
                .timeout(HttpTargetSupport.timeout(target));

        for (Map.Entry<String, String> h : HttpTargetSupport.headers(target).entrySet()) {
            reqBuilder.header(h.getKey(), templatize(h.getValue(), runContext));
        }

        String method = HttpTargetSupport.method(target);
        String body = templatize(HttpTargetSupport.body(target), runContext);
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        reqBuilder.method(method, publisher);

        HttpResponse<Void> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
        long latency = System.nanoTime() - start;
        int status = response.statusCode();
        List<Integer> expect = HttpTargetSupport.expectStatus(target);
        boolean ok = expect.contains(status);
        String err = ok ? null : "unexpected status " + status;
        return new SampleOutcome(ok, latency, status, err);
    }

    /** 场景 url/body/header 中可用 {@code ${iteration}}、{@code ${threadIndex}}。 */
    private static String templatize(String raw, LoadTestRunContext runContext) {
        if (raw == null) {
            return null;
        }
        return raw.replace("${iteration}", Long.toString(runContext.iteration()))
                .replace("${threadIndex}", Integer.toString(runContext.threadIndex()));
    }
}
