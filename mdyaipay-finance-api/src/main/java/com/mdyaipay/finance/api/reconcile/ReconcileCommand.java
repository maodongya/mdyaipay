package com.mdyaipay.finance.api.reconcile;

import java.io.Serializable;

/**
 * 发起一次渠道对账。
 * <p>{@code businessDate} 为 {@code yyyy-MM-dd}。{@code billSource} 为 {@code FILE} 或 {@code CHANNEL_PULL}。
 * 文件来源时 {@code filePath} 为本地 CSV 路径。</p>
 */
public final class ReconcileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String channel;
    private final String businessDate;
    private final String billSource;
    private final String filePath;

    /**
     * @param channel      渠道编码
     * @param businessDate 业务日
     * @param billSource   账单来源
     * @param filePath     CSV 路径；渠道拉取时可为 null
     */
    public ReconcileCommand(String channel, String businessDate, String billSource, String filePath) {
        this.channel = channel;
        this.businessDate = businessDate;
        this.billSource = billSource;
        this.filePath = filePath;
    }

    public String getChannel() {
        return channel;
    }

    public String getBusinessDate() {
        return businessDate;
    }

    public String getBillSource() {
        return billSource;
    }

    public String getFilePath() {
        return filePath;
    }
}
