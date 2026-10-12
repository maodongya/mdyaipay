package com.mdyaipay.accounting.observe;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.tools.logging.AlertEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 账务失败路径的稳定事件日志，供 Kibana 规则检索。
 * <p>
 * 不负责改钱包状态或映射错误码。
 */
public final class WalletAlertLog {

    private static final Logger LOG = LogManager.getLogger(WalletAlertLog.class);

    /** 禁止实例化。 */
    private WalletAlertLog() {
    }

    /**
     * 记录可预期的分录失败。
     * <p>幂等：每次调用写一条日志。</p>
     */
    public static void entryFailed(AccountingBusinessException ex) {
        if (ex.getErrorCode() == AccountingErrorCodes.VERSION_CONFLICT) {
            LOG.warn("{} error_code={}", AlertEvents.failure(AlertEvents.WALLET_VERSION_CONFLICT), ex.getErrorCode());
            return;
        }
        LOG.error("{} error_code={}", AlertEvents.failure(AlertEvents.WALLET_ENTRY_FAILED), ex.getErrorCode());
    }

    /**
     * 记录未预期异常。
     * <p>幂等：每次调用写一条日志，并带堆栈。</p>
     */
    public static void unexpected(Exception ex) {
        LOG.error(AlertEvents.failure(AlertEvents.WALLET_ENTRY_FAILED), ex);
    }
}
