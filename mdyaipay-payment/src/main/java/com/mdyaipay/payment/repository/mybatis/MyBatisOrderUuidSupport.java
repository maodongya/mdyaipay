package com.mdyaipay.payment.repository.mybatis;

import com.mdyaipay.tools.id.UuidV7Generator;

/**
 * MyBatis 仓储写入前解析行级 UUIDv7 主键：新单生成，已存在单复用库内 id。
 */
final class MyBatisOrderUuidSupport {

    /** 禁止实例化。 */
    private MyBatisOrderUuidSupport() {
    }

    /**
     * @param existingRowId 按业务单号查到的既有 id；无行时为 null
     * @param idGenerator   UUIDv7 生成器
     * @return 本次 upsert 应使用的 16 字节主键
     */
    static byte[] resolveRowId(byte[] existingRowId, UuidV7Generator idGenerator) {
        if (existingRowId != null) {
            return existingRowId;
        }
        return idGenerator.nextBytes();
    }
}
