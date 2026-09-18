package com.mdyaipay.user.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * 启动时执行商户域 DDL（与 payment 模块 init-schema 模式一致）。
 * <p>全量 {@code schema-merchant-mysql.sql} + 增量 {@code patch-merchant-mysql.sql}（补商户凭证表等）。</p>
 */
public final class UserSchemaInitializer {

    private UserSchemaInitializer() {
    }

    /**
     * @param dataSource  数据源
     * @param resetSchema true 时先 DROP 商户域表再 CREATE
     */
    public static void apply(DataSource dataSource, boolean resetSchema) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        if (isMySql(dataSource) && resetSchema) {
            executeScript(dataSource, "db/reset-merchant-mysql.sql", false);
        }
        executeScript(dataSource, "db/schema-merchant-mysql.sql", false);
        if (isMySql(dataSource)) {
            executeScript(dataSource, "db/patch-merchant-mysql.sql", true);
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

    private static void executeScript(DataSource dataSource, String classpathResource, boolean ignoreDuplicate) {
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
                if (ignoreDuplicate && (isDuplicateIndex(ex) || isDuplicateColumn(ex))) {
                    continue;
                }
                throw new IllegalStateException("schema statement failed: " + sql, ex);
            }
        }
    }

    private static boolean isDuplicateIndex(SQLException ex) {
        return ex.getErrorCode() == 1061;
    }

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
