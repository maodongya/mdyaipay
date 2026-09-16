package com.mdyaipay.tools.loadtest.http;

import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriver;

/**
 * HTTP 协议驱动：{@link java.net.http.HttpClient}，共享连接由 {@link HttpClientFactory} 按 target 缓存。
 * <p>
 * <b>不负责</b> 网关路由或鉴权逻辑——URL 与 Header 由场景 {@code target} 指定。
 */
public final class HttpLoadTestDriver implements LoadTestDriver {

    private final HttpRequestExecutor executor = new HttpRequestExecutor();

    @Override
    public String protocol() {
        return "http";
    }

    @Override
    public SampleOutcome execute(LoadTestPlan plan, LoadTestRunContext runContext) throws Exception {
        return executor.execute(plan.target(), runContext);
    }
}
