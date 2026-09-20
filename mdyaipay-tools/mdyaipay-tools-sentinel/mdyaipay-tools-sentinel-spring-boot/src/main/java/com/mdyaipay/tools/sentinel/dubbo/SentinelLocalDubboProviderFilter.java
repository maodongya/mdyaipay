package com.mdyaipay.tools.sentinel.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Provider 本机 Sentinel 限流（order 早于 Redis 整体限流 Filter）。
 */
@Activate(group = CommonConstants.PROVIDER, order = -8600)
public final class SentinelLocalDubboProviderFilter implements Filter {

    private final SentinelLocalDubboFilterSupport support;

    /**
     * @param support 共用 entry 逻辑
     */
    public SentinelLocalDubboProviderFilter(SentinelLocalDubboFilterSupport support) {
        this.support = support;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        support.entryOrThrow("provider", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
