package com.scaffold.mes.base.infra.integration;

import java.util.List;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.share.integration.mes.base.provider.BomConsumePort;
import com.scaffold.share.integration.mes.base.response.BomConsumeLine;

import lombok.RequiredArgsConstructor;

/**
 * BomConsumePortImpl 出站反冲的 BOM 行
 * <p>
 * 只留下当前站点、指定消耗方式、并且打开反冲的行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class BomConsumePortImpl implements BomConsumePort {

    private final BomItemRepository items;

    @Override
    public List<BomConsumeLine> perLot(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_LOT");
    }

    @Override
    public List<BomConsumeLine> perWafer(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_WAFER");
    }

    @Override
    public List<BomConsumeLine> perDie(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_DIE");
    }

    @Override
    public List<BomConsumeLine> perStrip(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_STRIP");
    }

    @Override
    public List<BomConsumeLine> perTime(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_TIME");
    }

    @Override
    public List<BomConsumeLine> perRun(Long bomVersionId, Long stepId) {
        return lines(bomVersionId, stepId, "PER_RUN");
    }

    private List<BomConsumeLine> lines(Long bomVersionId, Long stepId, String mode) {
        if (bomVersionId == null || stepId == null) {
            return List.of();
        }
        return items.listByVersion(bomVersionId).stream()
                .filter(row -> stepId.equals(row.getStepId()))
                .filter(row -> mode.equals(row.getConsumeMode()))
                .filter(row -> row.getBackflush() != null && row.getBackflush() == 1)
                .filter(row -> row.getQtyPer() != null && row.getQtyPer().signum() > 0)
                .map(this::line)
                .toList();
    }

    private BomConsumeLine line(BomItem row) {
        return new BomConsumeLine(row.getMaterialId(), row.getQtyPer());
    }
}
