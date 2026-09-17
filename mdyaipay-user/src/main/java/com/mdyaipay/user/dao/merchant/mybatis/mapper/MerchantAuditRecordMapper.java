package com.mdyaipay.user.dao.merchant.mybatis.mapper;

import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantAuditRecordRow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户审核记录表 MyBatis Mapper。
 */
@Mapper
public interface MerchantAuditRecordMapper {

    void insert(MerchantAuditRecordRow row);
}
