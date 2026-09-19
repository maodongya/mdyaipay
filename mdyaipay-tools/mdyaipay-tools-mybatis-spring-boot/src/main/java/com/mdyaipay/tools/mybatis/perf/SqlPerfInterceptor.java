package com.mdyaipay.tools.mybatis.perf;

import com.mdyaipay.tools.mybatis.autoconfigure.SqlPerfProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis 插件：在 {@link Executor} 的 query/update 返回后记录 SQL 耗时。
 * <p>
 * 仅拦截四参 {@code query}：六参重载由内层调用，双签名会导致同一条 SQL 打两次 {@code [SqlPerf]}。
 * <b>不负责</b>记录绑定参数（避免 PII 落盘）；SQL 文本为 Mapper 解析后的模板。
 */
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "update",
            args = {MappedStatement.class, Object.class}),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlPerfInterceptor implements Interceptor {

    private final SqlPerfProperties properties;

    /**
     * @param properties 性能日志配置；非 null
     */
    public SqlPerfInterceptor(SqlPerfProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.isEnabled()) {
            return invocation.proceed();
        }
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        long startNanos = System.nanoTime();
        Object result = null;
        Throwable error = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            SqlPerfLog.logIfNeeded(
                    properties.getSlowThresholdMillis(),
                    properties.isIncludeTraceId(),
                    mappedStatement,
                    boundSql.getSql(),
                    durationMillis,
                    result,
                    error);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
