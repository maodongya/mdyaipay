package com.mdyaipay.financegateway.api.withhold;

import java.io.Serializable;

/** 代扣渠道受理响应。 */
public final class ChannelWithholdView implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean accepted;

    public ChannelWithholdView() {
    }

    public ChannelWithholdView(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
