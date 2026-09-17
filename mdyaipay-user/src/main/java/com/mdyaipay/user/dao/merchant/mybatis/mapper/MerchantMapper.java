package com.mdyaipay.user.dao.merchant.mybatis.mapper;

import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商户主体表 MyBatis Mapper。
 */
@Mapper
public interface MerchantMapper {

    void upsert(MerchantRow row);

    MerchantRow findById(@Param("merchantId") long merchantId);
}
