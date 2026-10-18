package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepTool;
import com.scaffold.mes.base.domain.entity.StepTool;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepToolRepository;
import com.scaffold.mes.base.domain.repository.StepToolRepository;
import com.scaffold.mes.base.domain.repository.ToolRepository;

import lombok.RequiredArgsConstructor;

/**
 * StepToolApplicationService 站点工具要求
 * <p>
 * 产品专用要求优先。Route 步骤上有要求时，整组替换站点要求。都没有时不限制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepToolApplicationService {

    private final StepToolRepository requirements;
    private final RouteStepRepository routeSteps;
    private final RouteStepToolRepository routeTools;
    private final ToolRepository tools;

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
    public boolean accepted(Long stepId, Long productId, Long toolId, Long routeVersionId, Integer routeSeqNo) {
        List<Need> needs = routeNeeds(routeVersionId, routeSeqNo);
        if (needs.isEmpty()) {
            needs = requirements.listByStep(stepId).stream()
                    .map(row -> new Need(row.getProductId(), row.getToolTypeId()))
                    .toList();
        }
        List<Need> specific = needs.stream()
                .filter(row -> productId != null && productId.equals(row.productId()))
                .toList();
        List<Need> chosen = specific.isEmpty()
                ? needs.stream().filter(row -> row.productId() == null || row.productId() == 0L).toList()
                : specific;
        if (chosen.isEmpty()) {
            return true;
        }
        if (toolId == null) {
            return false;
        }
        Tool tool = tools.findById(toolId).orElse(null);
        if (tool == null) {
            return false;
        }
        return chosen.stream().anyMatch(row -> row.typeId().equals(tool.getTypeId()));
    }

    private List<Need> routeNeeds(Long routeVersionId, Integer routeSeqNo) {
        if (routeVersionId == null || routeSeqNo == null) {
            return List.of();
        }
        RouteStep step = routeSteps.findBySeq(routeVersionId, routeSeqNo).orElse(null);
        if (step == null) {
            return List.of();
        }
        return routeTools.listByRouteStep(step.getId()).stream()
                .map(row -> new Need(row.getProductId(), row.getToolTypeId()))
                .toList();
    }

    private record Need(Long productId, Long typeId) {
    }
}
