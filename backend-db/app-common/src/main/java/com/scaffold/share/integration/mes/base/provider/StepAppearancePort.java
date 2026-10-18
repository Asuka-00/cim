package com.scaffold.share.integration.mes.base.provider;

/**
 * StepAppearancePort 站点在 Route 里出现的次数
 * <p>
 * 执行模块不能直接读 Route 步骤。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepAppearancePort {

    /**
     * 这个站点在版本里出现几次。
     *
     * @param routeVersionId Route 版本
     * @param stepId 站点
     * @return 出现次数，没有时为 0
     */
    int count(Long routeVersionId, Long stepId);
}
