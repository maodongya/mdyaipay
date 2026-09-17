package com.mdyaipay.user.dao.merchant;

import com.mdyaipay.user.dao.merchant.mybatis.mapper.MerchantAuditRecordMapper;
import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantAuditRecordRow;
import com.mdyaipay.user.domain.merchant.MerchantAuditRecord;
import com.mdyaipay.user.domain.merchant.MerchantAuditRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/** 审核流水表：仅 insert，无 update。 */
@Repository
public class MyBatisMerchantAuditRecordRepository implements MerchantAuditRecordRepository {

    private final MerchantAuditRecordMapper auditRecordMapper;

    public MyBatisMerchantAuditRecordRepository(MerchantAuditRecordMapper auditRecordMapper) {
        this.auditRecordMapper = Objects.requireNonNull(auditRecordMapper);
    }

    @Override
    public MerchantAuditRecord save(MerchantAuditRecord record) {
        auditRecordMapper.insert(new MerchantAuditRecordRow(
                record.getRecordId(),
                record.getMerchantId(),
                record.getAuditor(),
                record.getResult(),
                record.getRemark(),
                record.getCreatedAt()));
        return record;
    }
}
