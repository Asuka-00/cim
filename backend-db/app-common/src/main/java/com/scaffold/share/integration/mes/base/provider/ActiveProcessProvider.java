package com.scaffold.share.integration.mes.base.provider;

import com.scaffold.share.integration.mes.base.response.FrozenProcess;

/**
 * ActiveProcessProvider 生效工艺查询
 * <p>
 * 由 app-mes-base 实现。执行模块下工单时经此契约固化版本，不直接访问主数据表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ActiveProcessProvider {

    /**
     * 取生效产品版本当前绑定的 ACTIVE Route 与 BOM，并核对 BOM 行与 Route 步骤一致。
     *
     * @param productVersionId 产品版本
     * @return 要固化到工单的版本
     */
    FrozenProcess freeze(Long productVersionId);
}
