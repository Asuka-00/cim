package com.scaffold.share.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.scaffold.share.context.CurrentSessionProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;

/**
 * TenantLineHandlerImpl租户处理器
 * <p>
 * 负责提供租户隔离相关核心能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Slf4j
@RequiredArgsConstructor
public class TenantLineHandlerImpl implements TenantLineHandler {

    private final CurrentSessionProvider currentSessionProvider;

    /**
     * 获取租户标识
     */
    @Override
    public Expression getTenantId() {
        String tenantId = currentSessionProvider.getCurrentTenantIdOrDefault();
        return new net.sf.jsqlparser.expression.StringValue(tenantId);
    }

    /**
     * 判断表
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 租户表、全局用户身份、用户-租户成员资格、权限目录不按行上的 tenant_id 过滤
        return "sys_tenant".equalsIgnoreCase(tableName)
                || "sys_user".equalsIgnoreCase(tableName)
                || "sys_user_tenant".equalsIgnoreCase(tableName)
                || "sys_permission".equalsIgnoreCase(tableName);
    }

}
