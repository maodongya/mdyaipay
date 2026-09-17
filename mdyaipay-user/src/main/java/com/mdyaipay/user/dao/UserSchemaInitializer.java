package com.mdyaipay.user.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * 启动时执行 {@code db/schema-merchant-mysql.sql}（与 payment 模块 init-schema 模式一致）。
 */
public final class UserSchemaInitializer {

    private UserSchemaInitializer() {
    }

    public static void apply(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        executeScript(dataSource, "db/schema-merchant-mysql.sql");
    }

    private static void executeScript(DataSource dataSource, String classpathResource) {
        String ddl;
        try (var in = UserSchemaInitializer.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("classpath " + classpathResource + " not found");
            }
            ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read " + classpathResource, ex);
        }
        for (String statement : ddl.split(";")) {
            String sql = stripSqlComments(statement).trim();
            if (sql.isEmpty()) {
                continue;
            }
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute(sql);
            } catch (SQLException ex) {
                throw new IllegalStateException("schema statement failed: " + sql, ex);
            }
        }
    }

    private static String stripSqlComments(String block) {
        var out = new StringBuilder();
        for (String line : block.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }
}
