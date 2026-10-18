package com.scaffold.mes.base.infra.integration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepCollectItemRepository;
import com.scaffold.share.integration.mes.base.provider.CollectSpecPort;
import com.scaffold.share.integration.mes.base.response.CollectLimit;

import lombok.RequiredArgsConstructor;

/**
 * CollectSpecPortImpl 采集规格
 * <p>
 * 步骤上有覆盖时整组替换站点规格。覆盖内部仍是产品专用优先。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class CollectSpecPortImpl implements CollectSpecPort {

    private final StepCollectItemRepository items;
    private final RouteStepRepository routeSteps;
    private final RouteStepCollectItemRepository routeItems;

    @Override
    public Optional<CollectLimit> limit(Long stepId, Long itemId, Long productId) {
        long product = productId == null ? 0L : productId;
        StepCollectItem item = items.find(stepId, itemId, product).orElse(null);
        if (item == null && product != 0L) {
            item = items.find(stepId, itemId, 0L).orElse(null);
        }
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(new CollectLimit(item.getLsl(), item.getUsl(),
                Integer.valueOf(1).equals(item.getAutoHoldOnOos()), item.getHoldReasonCode()));
    }

    @Override
    public List<Long> requiredItems(Long stepId, Long productId) {
        long product = productId == null ? 0L : productId;
        Map<Long, StepCollectItem> chosen = new LinkedHashMap<>();
        for (StepCollectItem item : items.listByStep(stepId)) {
            Long rowProduct = item.getProductId() == null ? 0L : item.getProductId();
            if (rowProduct != 0L && rowProduct != product) {
                continue;
            }
            StepCollectItem current = chosen.get(item.getItemId());
            if (current == null || (Long.valueOf(0L).equals(current.getProductId()) && rowProduct == product)) {
                chosen.put(item.getItemId(), item);
            }
        }
        return chosen.values().stream()
                .filter(row -> row.getIsRequired() == null || row.getIsRequired() == 1)
                .map(StepCollectItem::getItemId)
                .toList();
    }

    @Override
    public Optional<CollectLimit> limit(Long routeVersionId, Integer seqNo, Long stepId, Long itemId, Long productId) {
        List<RouteStepCollectItem> rows = routeRows(routeVersionId, seqNo);
        if (!rows.isEmpty()) {
            return routeLimit(rows, itemId, productId);
        }
        return limit(stepId, itemId, productId);
    }

    @Override
    public List<Long> requiredItems(Long routeVersionId, Integer seqNo, Long stepId, Long productId) {
        List<RouteStepCollectItem> rows = routeRows(routeVersionId, seqNo);
        if (!rows.isEmpty()) {
            return routeRequired(rows, productId);
        }
        return requiredItems(stepId, productId);
    }

    private List<RouteStepCollectItem> routeRows(Long routeVersionId, Integer seqNo) {
        if (routeVersionId == null || seqNo == null) {
            return List.of();
        }
        RouteStep step = routeSteps.findBySeq(routeVersionId, seqNo).orElse(null);
        if (step == null) {
            return List.of();
        }
        List<RouteStepCollectItem> rows = routeItems.listByRouteStep(step.getId());
        return rows == null ? List.of() : rows;
    }

    private Optional<CollectLimit> routeLimit(List<RouteStepCollectItem> rows, Long itemId, Long productId) {
        long product = productId == null ? 0L : productId;
        List<RouteStepCollectItem> forItem = rows.stream().filter(row -> itemId.equals(row.getItemId())).toList();
        RouteStepCollectItem item = forItem.stream().filter(row -> Long.valueOf(product).equals(row.getProductId())).findFirst()
                .orElse(null);
        if (item == null && product != 0L) {
            item = forItem.stream().filter(row -> row.getProductId() == null || row.getProductId() == 0L).findFirst()
                    .orElse(null);
        }
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(new CollectLimit(item.getLsl(), item.getUsl(),
                Integer.valueOf(1).equals(item.getAutoHoldOnOos()), item.getHoldReasonCode()));
    }

    private static List<Long> routeRequired(List<RouteStepCollectItem> rows, Long productId) {
        long product = productId == null ? 0L : productId;
        Map<Long, RouteStepCollectItem> chosen = new LinkedHashMap<>();
        for (RouteStepCollectItem item : rows) {
            long rowProduct = item.getProductId() == null ? 0L : item.getProductId();
            if (rowProduct != 0L && rowProduct != product) {
                continue;
            }
            RouteStepCollectItem current = chosen.get(item.getItemId());
            if (current == null || (Long.valueOf(0L).equals(current.getProductId()) && rowProduct == product)) {
                chosen.put(item.getItemId(), item);
            }
        }
        return chosen.values().stream()
                .filter(row -> row.getIsRequired() == null || row.getIsRequired() == 1)
                .map(RouteStepCollectItem::getItemId)
                .toList();
    }
}
