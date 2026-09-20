package com.mdyaipay.tools.ratelimit.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Consumer 出站限流：发起 RPC 前限流，避免压垮下游。
 */
@Activate(group = CommonConstants.CONSUMER, order = -8500)
public final class RateLimitDubboConsumerFilter implements Filter {

    private final RateLimitDubboFilterSupport support;

    /**
     * @param support 共用判定逻辑
     */
    public RateLimitDubboConsumerFilter(RateLimitDubboFilterSupport support) {
        this.support = support;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        support.checkOrThrow("consumer", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
