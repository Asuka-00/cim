package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepCollectItemApplicationService Route 步骤采集规格
 * <p>
 * 至少要有上限或下限。同一 Route 步骤、采集项和产品不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteStepCollectItemApplicationService {

    private final RouteStepRepository routeSteps;
    private final RouteStepCollectItemRepository items;

    /**
     * 列出某个版本的采集规格。
     *
     * @param routeVersionId Route 版本
     * @return 规格
     */
    public List<RouteStepCollectItem> list(Long routeVersionId) {
        return items.listByVersion(routeVersionId);
    }

    /**
     * 保存规格。
     *
     * @param request 版本、序号、采集项和上下限
     * @throws BusinessException 步骤不存在、上下限都空、自动 Hold 没写原因，或已经有规格
     */
    @Transactional
    public void save(RouteStepCollectItemSaveRequest request) {
        if (request.getLsl() == null && request.getUsl() == null) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_COLLECT_INVALID, "规格至少要有上限或下限");
        }
        boolean autoHold = Integer.valueOf(1).equals(request.getAutoHoldOnOos());
        if (autoHold && (request.getHoldReasonCode() == null || request.getHoldReasonCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_COLLECT_INVALID, "自动 Hold 必须填写原因码");
        }
        RouteStep step = routeSteps.findBySeq(request.getRouteVersionId(), request.getSeqNo()).orElse(null);
        if (step == null) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_COLLECT_INVALID, "Route 步骤不存在");
        }
        long productId = request.getProductId() == null ? 0L : request.getProductId();
        boolean exists = items.listByRouteStep(step.getId()).stream()
                .anyMatch(row -> request.getItemId().equals(row.getItemId()) && Long.valueOf(productId).equals(row.getProductId()));
        if (exists) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_COLLECT_INVALID, "这个步骤与采集项已有规格");
        }
        RouteStepCollectItem item = new RouteStepCollectItem();
        item.setRouteVersionId(request.getRouteVersionId());
        item.setRouteStepId(step.getId());
        item.setItemId(request.getItemId());
        item.setProductId(productId);
        item.setLsl(request.getLsl());
        item.setUsl(request.getUsl());
        item.setAutoHoldOnOos(autoHold ? 1 : 0);
        item.setHoldReasonCode(request.getHoldReasonCode());
        item.setIsRequired(Integer.valueOf(0).equals(request.getIsRequired()) ? 0 : 1);
        items.save(item);
    }
}
