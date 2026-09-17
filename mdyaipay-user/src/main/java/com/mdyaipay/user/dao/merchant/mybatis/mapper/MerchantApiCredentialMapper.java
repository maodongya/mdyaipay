package com.mdyaipay.user.dao.merchant.mybatis.mapper;

import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantApiCredentialRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商户 API 凭证表 MyBatis Mapper。
 */
@Mapper
public interface MerchantApiCredentialMapper {

    void upsert(MerchantApiCredentialRow row);

    MerchantApiCredentialRow findByAppKey(@Param("appKey") String appKey);
}
