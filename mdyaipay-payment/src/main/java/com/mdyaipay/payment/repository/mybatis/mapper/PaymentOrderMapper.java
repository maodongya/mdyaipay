package com.mdyaipay.payment.repository.mybatis.mapper;

import com.mdyaipay.payment.repository.mybatis.row.PaymentOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 表 {@code payment_order} 的 MyBatis Mapper。
 */
@Mapper
public interface PaymentOrderMapper {

    int upsert(PaymentOrderRow row);

    PaymentOrderRow findByOrderNo(@Param("orderNo") String orderNo);
}
