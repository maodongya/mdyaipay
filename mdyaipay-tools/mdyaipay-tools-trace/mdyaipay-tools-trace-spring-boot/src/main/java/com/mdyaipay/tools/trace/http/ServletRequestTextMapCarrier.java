package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.TextMapCarrier;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 从 {@link HttpServletRequest} 读取 Trace 传播头。
 */
public final class ServletRequestTextMapCarrier implements TextMapCarrier {

    private final HttpServletRequest request;

    public ServletRequestTextMapCarrier(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        return request.getHeader(key);
    }
}
