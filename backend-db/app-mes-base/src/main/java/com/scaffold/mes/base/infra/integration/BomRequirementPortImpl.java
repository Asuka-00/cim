package com.scaffold.mes.base.infra.integration;

import java.util.List;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.share.integration.mes.base.provider.BomRequirementPort;
import com.scaffold.share.integration.mes.base.response.BomRequirementLine;

import lombok.RequiredArgsConstructor;

/**
 * BomRequirementPortImpl 工单物料展开
 * <p>
 * 把 BOM 行转成展开用的用量和损耗率。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class BomRequirementPortImpl implements BomRequirementPort {

    private final BomItemRepository items;

    @Override
    public List<BomRequirementLine> list(Long bomVersionId) {
        if (bomVersionId == null) {
            return List.of();
        }
        return items.listByVersion(bomVersionId).stream()
                .map(row -> new BomRequirementLine(row.getId(), row.getMaterialId(), row.getConsumeMode(), row.getQtyPer(),
                        row.getScrapRate(), row.getStepId(), row.getRouteSeqNo()))
                .toList();
    }
}
