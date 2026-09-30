package com.mdyaipay.finance.repository.mybatis.mapper;

import com.mdyaipay.finance.repository.mybatis.row.ReconciliationBatchRow;
import com.mdyaipay.finance.repository.mybatis.row.ReconciliationDifferenceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对账批次与差异表 Mapper。
 */
@Mapper
public interface ReconciliationBatchMapper {

    /** 按渠道与业务日点查批次；无行时 null。 */
    ReconciliationBatchRow findBatch(@Param("channel") String channel, @Param("businessDate") String businessDate);

    /** 插入或按唯一键覆盖批次字段，不改已有主键。 */
    int upsertBatch(ReconciliationBatchRow row);

    /** 删除某批次下的全部差异。 */
    int deleteDifferences(@Param("batchId") long batchId);

    /** 插入一条差异。 */
    int insertDifference(ReconciliationDifferenceRow row);

    /** 按批次主键列出差异。 */
    List<ReconciliationDifferenceRow> findDifferences(@Param("batchId") long batchId);
}
