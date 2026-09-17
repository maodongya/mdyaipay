package com.mdyaipay.user.dao.support.mybatis;

import java.sql.Timestamp;
import java.time.Instant;

final class InstantJdbcSupport {

    private InstantJdbcSupport() {
    }

    static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
