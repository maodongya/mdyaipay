package com.mdyaipay.user.dao.merchant.mybatis.mapper;

import com.mdyaipay.user.dao.merchant.mybatis.row.ShopRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 店铺表 MyBatis Mapper。
 */
@Mapper
public interface ShopMapper {

    void upsert(ShopRow row);

    ShopRow findById(@Param("shopId") long shopId);
}
