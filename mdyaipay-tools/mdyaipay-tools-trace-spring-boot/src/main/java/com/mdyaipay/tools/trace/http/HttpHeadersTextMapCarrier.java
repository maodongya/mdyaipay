package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.TextMapCarrier;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Locale;

/**
 * 基于 Spring {@link HttpHeaders} 的只读/可写 Carrier。
 */
public final class HttpHeadersTextMapCarrier implements TextMapCarrier {

    private final HttpHeaders headers;

    public HttpHeadersTextMapCarrier(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        List<String> values = headers.get(key);
        if (values == null || values.isEmpty()) {
            values = headers.get(key.toLowerCase(Locale.ROOT));
        }
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public void set(String key, String value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            headers.remove(key);
        } else {
            headers.set(key, value);
        }
    }
}
