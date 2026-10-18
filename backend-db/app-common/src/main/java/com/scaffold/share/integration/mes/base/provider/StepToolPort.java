package com.scaffold.share.integration.mes.base.provider;

/**
 * StepToolPort 站点工具要求
 * <p>
 * 执行模块不能直接读要求表。没有要求时不限制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepToolPort {

    /**
     * 这个工具能否用于该站点和产品。
     *
     * @param stepId 站点
     * @param productId 产品
     * @param toolId 工具，没有要求时可空
     * @param routeVersionId Route 版本，可空
     * @param routeSeqNo 步骤序号，可空
     * @return 没有要求，或工具类型命中时为 true
     */
    boolean accepted(Long stepId, Long productId, Long toolId, Long routeVersionId, Integer routeSeqNo);
}
