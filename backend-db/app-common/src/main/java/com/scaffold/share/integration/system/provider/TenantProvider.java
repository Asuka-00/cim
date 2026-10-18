package com.scaffold.share.integration.system.provider;

import java.util.List;

import com.scaffold.share.integration.system.response.TenantView;

/**
 * 租户查询接口
 * <p>
 * 供调度系统查询需要调度的租户列表，由系统模块（app-system）实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-28
 */
public interface TenantProvider {

    /**
     * 查询可供调度处理的全部租户。
     */
    List<TenantView> findAllTenants();
}
