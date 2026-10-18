package com.scaffold.share.integration.mes.base.provider;

import java.util.List;

import com.scaffold.share.integration.mes.base.response.BomRequirementLine;

/**
 * BomRequirementPort 工单物料展开
 * <p>
 * 执行模块不能直接读 BOM。没有行时返回空列表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BomRequirementPort {

    /**
     * 列出一个 BOM 版本的全部行。
     *
     * @param bomVersionId BOM 版本
     * @return 行
     */
    List<BomRequirementLine> list(Long bomVersionId);
}
