package com.mdyaipay.payment.repository.mybatis;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * 应用启动时执行 classpath SQL 初始化 payment 表结构（可配置关闭）。
 */
public final class PaymentSchemaInitializer {

    private PaymentSchemaInitializer() {
    }

    /**
     * 执行建表脚本；MySQL 上可选先 reset，再 patch 与二级索引。
     *
     * @param dataSource  数据源
     * @param resetSchema true 时先 DROP 业务表再 CREATE
     */
    public static void apply(DataSource dataSource, boolean resetSchema) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        if (isMySql(dataSource) && resetSchema) {
            executeScript(dataSource, "db/reset-mysql.sql", false);
        }
        executeScript(dataSource, "db/schema-mysql.sql", false);
        if (isMySql(dataSource)) {
            executeScript(dataSource, "db/patch-mysql.sql", true);
            executeScript(dataSource, "db/indexes-mysql.sql", true);
        }
    }

    private static boolean isMySql(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to detect database product", ex);
        }
    }

    private static void executeScript(DataSource dataSource, String classpathResource, boolean ignoreDuplicateIndex) {
        String ddl;
        try (var in = PaymentSchemaInitializer.class.getClassLoader().getResourceAsStream(classpathResource)) {
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
                if (ignoreDuplicateIndex && (isDuplicateIndex(ex) || isDuplicateColumn(ex))) {
                    continue;
                }
                throw new IllegalStateException("schema statement failed: " + sql, ex);
            }
        }
    }

    /** MySQL/MariaDB：索引名已存在 */
    private static boolean isDuplicateIndex(SQLException ex) {
        return ex.getErrorCode() == 1061;
    }

    /** MySQL：列已存在 */
    private static boolean isDuplicateColumn(SQLException ex) {
        return ex.getErrorCode() == 1060;
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
