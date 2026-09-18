package com.mdyaipay.payment.repository.mybatis.mapper;

import com.mdyaipay.payment.repository.mybatis.row.PayoutOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 表 {@code payout_order} 的 MyBatis Mapper。
 */
@Mapper
public interface PayoutOrderMapper {

    int upsert(PayoutOrderRow row);

    PayoutOrderRow findByPayoutNo(@Param("payoutNo") String payoutNo);
}
