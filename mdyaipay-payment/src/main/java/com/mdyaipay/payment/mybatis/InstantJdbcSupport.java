package com.mdyaipay.payment.mybatis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class InstantJdbcSupport {

    private InstantJdbcSupport() {
    }

    public static Timestamp toTimestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    public static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        if (ts == null) {
            throw new SQLException("null timestamp for column " + column);
        }
        return ts.toInstant();
    }
}
