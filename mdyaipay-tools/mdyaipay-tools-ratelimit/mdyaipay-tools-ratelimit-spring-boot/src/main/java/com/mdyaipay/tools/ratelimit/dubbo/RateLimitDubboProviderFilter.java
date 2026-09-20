package com.mdyaipay.tools.ratelimit.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Provider 入站限流：在业务实现前 {@link RateLimitDubboFilterSupport#checkOrThrow}。
 */
@Activate(group = CommonConstants.PROVIDER, order = -8500)
public final class RateLimitDubboProviderFilter implements Filter {

    private final RateLimitDubboFilterSupport support;

    /**
     * @param support 共用判定逻辑
     */
    public RateLimitDubboProviderFilter(RateLimitDubboFilterSupport support) {
        this.support = support;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        support.checkOrThrow("provider", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
