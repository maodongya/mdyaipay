package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.MerchantAuditRecord;
import com.mdyaipay.user.domain.merchant.MerchantAuditRecordRepository;

public final class MapMerchantAuditRecordRepository implements MerchantAuditRecordRepository {

    @Override
    public MerchantAuditRecord save(MerchantAuditRecord record) {
        return record;
    }
}
