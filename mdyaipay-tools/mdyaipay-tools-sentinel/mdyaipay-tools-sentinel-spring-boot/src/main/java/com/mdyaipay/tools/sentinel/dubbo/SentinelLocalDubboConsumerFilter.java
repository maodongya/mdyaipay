package com.mdyaipay.tools.sentinel.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Consumer 本机 Sentinel 限流。
 */
@Activate(group = CommonConstants.CONSUMER, order = -8600)
public final class SentinelLocalDubboConsumerFilter implements Filter {

    private final SentinelLocalDubboFilterSupport support;

    /**
     * @param support 共用 entry 逻辑
     */
    public SentinelLocalDubboConsumerFilter(SentinelLocalDubboFilterSupport support) {
        this.support = support;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        support.entryOrThrow("consumer", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
