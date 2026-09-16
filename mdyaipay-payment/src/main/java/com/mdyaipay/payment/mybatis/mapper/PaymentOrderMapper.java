package com.mdyaipay.payment.mybatis.mapper;

import com.mdyaipay.payment.mybatis.row.PaymentOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentOrderMapper {

    int upsert(PaymentOrderRow row);

    PaymentOrderRow findByOrderNo(@Param("orderNo") String orderNo);
}
