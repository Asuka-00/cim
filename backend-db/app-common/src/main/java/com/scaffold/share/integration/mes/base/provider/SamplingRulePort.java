package com.scaffold.share.integration.mes.base.provider;

import java.util.Optional;

import com.scaffold.share.integration.mes.base.response.SamplingBinding;

/**
 * SamplingRulePort 站点抽检规则
 * <p>
 * 执行模块不能直接读规则。产品专用绑定优先于通用绑定。没有启用规则时为空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface SamplingRulePort {

    /**
     * 取站点上生效的间隔规则。
     *
     * @param stepId 量测站点
     * @param productId 产品。空按通用
     * @return 规则和间隔，没有时为空
     */
    Optional<SamplingBinding> binding(Long stepId, Long productId);

    /**
     * 取这一步生效的间隔规则。步骤上有覆盖时，不再使用站点绑定。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @param stepId 量测站点
     * @param productId 产品。空按通用
     * @return 规则和间隔，没有时为空
     */
    Optional<SamplingBinding> binding(Long routeVersionId, Integer seqNo, Long stepId, Long productId);
}
