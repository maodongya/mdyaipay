package com.mdyaipay.financegateway.api.collect;

import java.io.Serializable;

/**
 * 收单渠道受理响应。
 */
public final class ChannelCollectView implements Serializable {

    private static final long serialVersionUID = 1L;

    private String submitResult;
    /** 渠道交易号；同步成功时非空。 */
    private String channelTradeNo;

    public ChannelCollectView() {
    }

    public ChannelCollectView(String submitResult, String channelTradeNo) {
        this.submitResult = submitResult;
        this.channelTradeNo = channelTradeNo;
    }

    public String getSubmitResult() {
        return submitResult;
    }

    public void setSubmitResult(String submitResult) {
        this.submitResult = submitResult;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public void setChannelTradeNo(String channelTradeNo) {
        this.channelTradeNo = channelTradeNo;
    }
}
