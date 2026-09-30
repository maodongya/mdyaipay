package com.mdyaipay.payment.repository.mybatis.mapper;

import com.mdyaipay.payment.repository.mybatis.row.PaymentOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * 表 {@code payment_order} 的 MyBatis Mapper。
 */
@Mapper
public interface PaymentOrderMapper {

    int upsert(PaymentOrderRow row);

    PaymentOrderRow findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 按渠道与成功时间窗查询已带渠道交易号的成功收单。
     */
    List<PaymentOrderRow> findCollectSuccess(
            @Param("channel") String channel,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
