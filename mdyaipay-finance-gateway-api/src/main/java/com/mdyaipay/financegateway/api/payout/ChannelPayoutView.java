package com.mdyaipay.financegateway.api.payout;

import java.io.Serializable;

/** 代付渠道受理响应。 */
public final class ChannelPayoutView implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean accepted;

    public ChannelPayoutView() {
    }

    public ChannelPayoutView(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
