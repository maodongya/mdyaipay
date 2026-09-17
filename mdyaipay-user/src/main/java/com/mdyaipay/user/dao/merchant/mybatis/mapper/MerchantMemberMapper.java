package com.mdyaipay.user.dao.merchant.mybatis.mapper;

import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantMemberRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商户成员表 MyBatis Mapper。
 */
@Mapper
public interface MerchantMemberMapper {

    void upsert(MerchantMemberRow row);

    MerchantMemberRow findByMerchantIdAndUserId(
            @Param("merchantId") long merchantId,
            @Param("userId") long userId);
}
