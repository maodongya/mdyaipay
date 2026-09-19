package com.mdyaipay.tools.trace.dubbo;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.http.IncomingTraceResolver;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Objects;
import java.util.Optional;

/**
 * Dubbo Provider：从 attachment 续链或新起 trace，绑定 {@link TraceContext} 与 MDC，调用结束后清理。
 */
@Activate(group = CommonConstants.PROVIDER, order = -9000)
public class TraceDubboProviderFilter implements Filter {

    private final TraceMdcSupport mdcSupport;

    /**
     * @param mdcSupport MDC 写入器；非 null
     */
    public TraceDubboProviderFilter(TraceMdcSupport mdcSupport) {
        this.mdcSupport = Objects.requireNonNull(mdcSupport, "mdcSupport");
    }

    /**
     * 入站提取 Trace，执行业务，finally 恢复线程上下文。
     *
     * <p>幂等：嵌套 Dubbo 调用由 {@link TraceContext#restore} 恢复外层快照。
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        TraceSnapshot snapshot = IncomingTraceResolver.resolve(new RpcContextAttachmentCarrier(invocation));
        Optional<TraceSnapshot> previous = TraceContext.bind(snapshot);
        mdcSupport.put(snapshot);
        try {
            return invoker.invoke(invocation);
        } finally {
            mdcSupport.clear();
            TraceContext.restore(previous);
        }
    }
}
