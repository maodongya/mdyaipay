package com.mdyaipay.payment.domain.collect;

/**
 * 收单渠道受理结果：同步终态或等待渠道异步确认（如网银）。
 * <p>同步成功必须带渠道交易号。不负责落库。</p>
 */
public final class PaymentSubmitResult {

    /**
     * 渠道受理种类。
     */
    public enum Kind {
        /** 渠道同步成功。 */
        SYNC_SUCCESS,
        /** 渠道同步失败。 */
        SYNC_FAILURE,
        /** 等待渠道异步确认。 */
        AWAITING_CHANNEL_CONFIRMATION
    }

    private final Kind kind;
    private final String channelTradeNo;

    private PaymentSubmitResult(Kind kind, String channelTradeNo) {
        this.kind = kind;
        this.channelTradeNo = channelTradeNo;
    }

    /**
     * 同步成功。前置：{@code channelTradeNo} 非空白。
     */
    public static PaymentSubmitResult syncSuccess(String channelTradeNo) {
        if (channelTradeNo == null || channelTradeNo.isBlank()) {
            throw new IllegalArgumentException("channelTradeNo must not be blank");
        }
        return new PaymentSubmitResult(Kind.SYNC_SUCCESS, channelTradeNo);
    }

    /** 同步失败，不带渠道交易号。 */
    public static PaymentSubmitResult syncFailure() {
        return new PaymentSubmitResult(Kind.SYNC_FAILURE, null);
    }

    /** 等待异步确认，此时还没有渠道交易号。 */
    public static PaymentSubmitResult awaitingChannelConfirmation() {
        return new PaymentSubmitResult(Kind.AWAITING_CHANNEL_CONFIRMATION, null);
    }

    /** 受理种类。 */
    public Kind kind() {
        return kind;
    }

    /** 渠道交易号；非成功时为 null。 */
    public String channelTradeNo() {
        return channelTradeNo;
    }
}
