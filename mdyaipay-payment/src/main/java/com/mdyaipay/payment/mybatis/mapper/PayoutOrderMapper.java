package com.mdyaipay.payment.mybatis.mapper;

import com.mdyaipay.payment.mybatis.row.PayoutOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayoutOrderMapper {

    int upsert(PayoutOrderRow row);

    PayoutOrderRow findByPayoutNo(@Param("payoutNo") String payoutNo);
}
