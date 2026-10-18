package com.scaffold.mes.base.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.RouteStep;

/**
 * BomRouteAlignment BOM 与 Route 对齐检查
 * <p>
 * BOM 行的站点必须出现在 Route 中。序号不为 0 时，该序号上的站点也必须一致。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
final class BomRouteAlignment {

    private BomRouteAlignment() {
    }

    /**
     * 找出第一条对不上的 BOM 行。
     *
     * @param steps 当前要比对的 Route 步骤
     * @param items BOM 行
     * @return 冲突行号，全部对齐时为空
     */
    static Optional<Integer> firstMismatch(List<RouteStep> steps, List<BomItem> items) {
        Map<Integer, Long> stepBySeq = steps.stream()
                .collect(Collectors.toMap(RouteStep::getSeqNo, RouteStep::getStepId, (left, right) -> left));
        for (BomItem item : items) {
            boolean stepMissing = item.getStepId() == null || steps.stream().noneMatch(step -> item.getStepId().equals(step.getStepId()));
            boolean seqMismatch = item.getStepId() != null && item.getRouteSeqNo() != null && item.getRouteSeqNo() != 0
                    && !item.getStepId().equals(stepBySeq.get(item.getRouteSeqNo()));
            if (stepMissing || seqMismatch) {
                return Optional.of(item.getLineNo());
            }
        }
        return Optional.empty();
    }
}
