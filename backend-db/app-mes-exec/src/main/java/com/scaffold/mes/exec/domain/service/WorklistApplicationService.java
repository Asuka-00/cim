package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.dto.WorklistItem;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.integration.mes.base.provider.EquipmentQueueProvider;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;

import lombok.RequiredArgsConstructor;

/**
 * WorklistApplicationService 作业清单
 * <p>
 * 只列出未 Hold 的 WAITING Lot。优先级数字小的在前，相同优先级按进入站点的时间先后。指定设备时只保留该设备能做的站点。当前用户有站点或区域范围时，范围外的 Lot 不出现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WorklistApplicationService {

    private final LotRepository lotRepository;
    private final EquipmentQueueProvider equipmentQueue;
    private final DispatchRuleApplicationService dispatchRules;
    private final UserScopePort scopes;
    private final CurrentSessionProvider session;

    /**
     * 查询待加工队列。
     *
     * @param stepId 站点，空表示不限
     * @param areaId 区域，空表示不限
     * @param equipmentId 设备，空表示不限
     * @param ruleId 排序规则，空则用默认规则
     * @return 排序后的队列
     */
    public List<WorklistItem> list(Long stepId, Long areaId, Long equipmentId, Long ruleId) {
        List<Long> steps = equipmentId == null ? List.of() : equipmentQueue.stepIds(equipmentId);
        String user = session.getCurrentUserNameOrDefault();
        return lotRepository.listWaiting(stepId, areaId).stream()
                .filter(lot -> equipmentId == null || steps.contains(lot.getStepId()))
                .filter(lot -> scopes.allowed(user, "STEP", lot.getStepId()))
                .filter(lot -> !"AREA".equals(lot.getLocationType())
                        || scopes.allowed(user, "AREA", lot.getLocationId()))
                .sorted(dispatchRules.comparator(ruleId))
                .map(lot -> new WorklistItem(lot.getId(), lot.getLotNo(), lot.getPriority(), lot.getIsHot(),
                        lot.getStepId(), lot.getStepEnterAt(), lot.getQty()))
                .toList();
    }
}
