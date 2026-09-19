package com.mdyaipay.tools.trace.dubbo;

import com.mdyaipay.tools.trace.TextMapCarrier;
import org.apache.dubbo.rpc.Invocation;

import java.util.Objects;

/**
 * 将 Dubbo {@link Invocation} attachment 适配为 {@link TextMapCarrier}。
 * <p>
 * Consumer 可写；Provider 通常只读提取。不负责 RpcContext 其它通道。
 */
final class RpcContextAttachmentCarrier implements TextMapCarrier {

    private final Invocation invocation;

    /**
     * @param invocation 当前 RPC 调用；非 null
     */
    RpcContextAttachmentCarrier(Invocation invocation) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
    }

    /**
     * 读取 attachment；Dubbo 无该键时返回 null。
     */
    @Override
    public String get(String key) {
        return invocation.getAttachment(key);
    }

    /**
     * 写入 attachment，供 Consumer 出站注入。
     */
    @Override
    public void set(String key, String value) {
        invocation.setAttachment(key, value);
    }
}
