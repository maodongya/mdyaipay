package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.Propagation;
import com.mdyaipay.tools.trace.TextMapCarrier;

import java.net.http.HttpRequest;
import java.util.function.BiConsumer;

/**
 * JDK {@link HttpClient} 出站请求注入 Trace 头。
 */
public final class OutgoingHttpTraceSupport {

    private OutgoingHttpTraceSupport() {
    }

    /**
     * 将当前 {@link com.mdyaipay.tools.trace.TraceContext} 写入 {@link HttpRequest.Builder}。
     */
    public static void inject(HttpRequest.Builder builder) {
        Propagation.inject(headerCarrier(builder));
    }

    private static TextMapCarrier headerCarrier(HttpRequest.Builder builder) {
        BiConsumer<String, String> setter = builder::header;
        return new TextMapCarrier() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void set(String key, String value) {
                if (key != null && value != null) {
                    setter.accept(key, value);
                }
            }
        };
    }
}
