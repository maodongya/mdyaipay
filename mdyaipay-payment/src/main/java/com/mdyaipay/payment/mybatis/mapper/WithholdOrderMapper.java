package com.mdyaipay.payment.mybatis.mapper;

import com.mdyaipay.payment.mybatis.row.WithholdOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 表 {@code withhold_order} 的 MyBatis Mapper。
 */
@Mapper
public interface WithholdOrderMapper {

    int upsert(WithholdOrderRow row);

    WithholdOrderRow findByDeductionNo(@Param("deductionNo") String deductionNo);
}
