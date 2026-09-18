package com.mdyaipay.tools.trace.dubbo;

import com.mdyaipay.tools.trace.Propagation;
import com.mdyaipay.tools.trace.TraceContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Consumer：将当前 {@link TraceContext} 注入 RPC attachment（W3C {@code traceparent} 等）。
 * <p>
 * 无上下文时不写 attachment。不负责 Provider 侧提取或 MDC。
 */
@Activate(group = CommonConstants.CONSUMER, order = -9000)
public class TraceDubboConsumerFilter implements Filter {

    /**
     * 出站前注入 Trace；随后委托下游 Invoker。
     *
     * <p>幂等：重复进入 Filter 会覆盖同键 attachment。
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        TraceContext.current().ifPresent(snapshot -> {
            RpcContextAttachmentCarrier carrier = new RpcContextAttachmentCarrier(invocation);
            Propagation.inject(carrier, snapshot);
        });
        return invoker.invoke(invocation);
    }
}
