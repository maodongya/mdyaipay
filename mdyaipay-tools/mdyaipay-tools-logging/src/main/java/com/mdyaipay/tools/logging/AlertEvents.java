package com.mdyaipay.tools.logging;

/**
 * Kibana 报警使用的稳定事件动作名。
 * <p>
 * 不负责采集、索引或规则执行；只保证日志消息可被 Filebeat 解析。
 */
public final class AlertEvents {

    /** 网关调用下游失败。 */
    public static final String GATEWAY_UPSTREAM_FAILED = "gateway_upstream_failed";
    /** 限流拒绝。 */
    public static final String RATE_LIMIT_DENIED = "rate_limit_denied";
    /** 限流后端异常。 */
    public static final String RATE_LIMIT_BACKEND_ERROR = "rate_limit_backend_error";
    /** 商户验签/认证失败。 */
    public static final String MERCHANT_AUTH_REJECTED = "merchant_auth_rejected";
    /** Outbox 投递失败。 */
    public static final String OUTBOX_RELAY_FAILED = "outbox_relay_failed";
    /** 支付渠道调用抛错。 */
    public static final String PAYMENT_CHANNEL_FAILED = "payment_channel_failed";
    /** 支付业务失败（同步失败或标记 FAILED）。 */
    public static final String PAYMENT_FAILED = "payment_failed";
    /** 账务通知失败。 */
    public static final String ACCOUNTING_NOTIFY_FAILED = "accounting_notify_failed";
    /** 钱包分录失败。 */
    public static final String WALLET_ENTRY_FAILED = "wallet_entry_failed";
    /** 钱包乐观锁冲突。 */
    public static final String WALLET_VERSION_CONFLICT = "wallet_version_conflict";
    /** 对账任务失败。 */
    public static final String RECONCILIATION_FAILED = "reconciliation_failed";
    /** 对账发现差异。 */
    public static final String RECONCILIATION_DIFFERENCE_FOUND = "reconciliation_difference_found";
    /** 渠道 HTTP 失败。 */
    public static final String CHANNEL_HTTP_FAILED = "channel_http_failed";
    /** 渠道 HTTP 超时。 */
    public static final String CHANNEL_HTTP_TIMEOUT = "channel_http_timeout";
    /** Mock 渠道模拟失败。 */
    public static final String MOCK_CHANNEL_FAILED = "mock_channel_failed";

    /**
     * 禁止实例化。
     */
    private AlertEvents() {
    }

    /**
     * 组装失败事件消息。
     * <p>
     * 幂等：纯函数，无副作用。
     *
     * @param action 稳定动作名，不能为空
     * @return 含 {@code event} 与 {@code outcome=failure} 的消息
     */
    public static String failure(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        return "event=" + action + " outcome=failure";
    }
}
