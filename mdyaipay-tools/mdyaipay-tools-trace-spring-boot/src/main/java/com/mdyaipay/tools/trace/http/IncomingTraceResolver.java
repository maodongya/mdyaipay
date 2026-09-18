package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.Propagation;
import com.mdyaipay.tools.trace.TextMapCarrier;
import com.mdyaipay.tools.trace.TraceSnapshot;

/**
 * 从入站 Carrier 解析 Trace，无头时新起根 trace。
 */
public final class IncomingTraceResolver {

    private IncomingTraceResolver() {
    }

    /**
     * 提取或新建本请求段快照。
     */
    public static TraceSnapshot resolve(TextMapCarrier incoming) {
        return Propagation.extract(incoming).orElseGet(TraceSnapshot::startNew);
    }
}
