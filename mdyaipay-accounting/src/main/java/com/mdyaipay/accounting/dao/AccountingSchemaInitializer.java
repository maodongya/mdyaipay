package com.mdyaipay.accounting.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * 启动时执行账务域 DDL（与 user/payment 模块 init-schema 模式一致）。
 */
public final class AccountingSchemaInitializer {

    private AccountingSchemaInitializer() {
    }

    /**
     * @param dataSource  数据源
     * @param resetSchema true 时先 DROP 账务表再 CREATE
     */
    public static void apply(DataSource dataSource, boolean resetSchema) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        if (isMySql(dataSource) && resetSchema) {
            executeScript(dataSource, "db/reset-accounting-mysql.sql", false);
        }
        executeScript(dataSource, "db/schema-accounting-mysql.sql", false);
    }

    private static boolean isMySql(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to detect database product", ex);
        }
    }

    private static void executeScript(DataSource dataSource, String classpathResource, boolean ignoreDuplicate) {
        String ddl;
        try (var in = AccountingSchemaInitializer.class.getClassLoader().getResourceAsStream(classpathResource)) {
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
                if (ignoreDuplicate && ex.getErrorCode() == 1061) {
                    continue;
                }
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
