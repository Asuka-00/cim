package com.scaffold.share.config;

import org.apache.ibatis.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.db.DatabaseKind;
import com.scaffold.share.logging.SqlSingleLineLog;

import jakarta.annotation.PostConstruct;

/**
 * MybatisPlusConfig配置类
 * <p>
 * 负责提供MybatisPlusConfig相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Configuration
public class MybatisPlusConfig {

    @Value("${logging.sql-log-enabled:true}")
    private boolean sqlLogEnabled;

    /**
     * 初始化 MyBatis SQL 日志
     */
    @PostConstruct
    public void initMybatisSqlLog() {
        LogFactory.useCustomLogging(SqlSingleLineLog.class);
        SqlSingleLineLog.setEnabled(sqlLogEnabled);
    }

    /**
     * 创建租户行拦截器
     */
    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(CurrentSessionProvider currentSessionProvider) {
        return new TenantLineInnerInterceptor(new TenantLineHandlerImpl(currentSessionProvider));
    }

    /**
     * 创建 MyBatis Plus 拦截器（分页方言随 {@link DatabaseKind} 切换）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            TenantLineInnerInterceptor tenantLineInnerInterceptor, DatabaseKind databaseKind) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(databaseKind.toMpDbType()));
        return interceptor;
    }

    /**
     * 自定义 SQL 注入器，注册 insertBatchSomeColumn 批量插入方法
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new BatchSqlInjector();
    }
}
