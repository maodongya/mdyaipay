package com.mdyaipay.payment.repository;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class PaymentSchemaInitializer {

    private PaymentSchemaInitializer() {
    }

    public static void apply(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        String ddl;
        try (var in = PaymentSchemaInitializer.class.getClassLoader().getResourceAsStream("db/schema-mysql.sql")) {
            if (in == null) {
                throw new IllegalStateException("classpath db/schema-mysql.sql not found");
            }
            ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read schema-mysql.sql", ex);
        }
        for (String statement : ddl.split(";")) {
            String sql = statement.trim();
            if (sql.isEmpty() || sql.startsWith("--")) {
                continue;
            }
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute(sql);
            } catch (SQLException ex) {
                throw new IllegalStateException("schema statement failed: " + sql, ex);
            }
        }
    }
}
