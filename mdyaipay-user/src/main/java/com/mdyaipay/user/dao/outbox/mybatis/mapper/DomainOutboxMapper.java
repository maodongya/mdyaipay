package com.mdyaipay.user.dao.outbox.mybatis.mapper;

import com.mdyaipay.user.dao.outbox.mybatis.row.DomainOutboxRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 领域 Outbox 表 MyBatis Mapper。
 */
@Mapper
public interface DomainOutboxMapper {

    void insert(DomainOutboxRow row);

    List<DomainOutboxRow> findPending(@Param("limit") int limit);

    void markSent(@Param("outboxId") long outboxId, @Param("sentAt") java.time.Instant sentAt);
}
