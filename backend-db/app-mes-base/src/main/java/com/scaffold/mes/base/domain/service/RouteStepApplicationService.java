package com.scaffold.mes.base.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepApplicationService Route 步骤应用服务
 * <p>
 * 只有草稿版本能改步骤。序号在版本内唯一，站点可以重复出现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteStepApplicationService {

    private static final Set<String> QUEUE_ACTIONS = Set.of("HOLD", "WARN");

    private final RouteStepRepository repository;
    private final RouteVersionRepository versionRepository;
    private final StepRepository stepRepository;
    private final CountUnitRepository countUnitRepository;
    private final EquipmentGroupRepository equipmentGroupRepository;

    /**
     * 列出某个版本的步骤。
     *
     * @param routeVersionId Route 版本
     * @return 按序号排序的步骤
     * @throws BusinessException 版本不存在
     */
    public List<RouteStep> list(Long routeVersionId) {
        requireVersion(routeVersionId);
        return repository.listByVersion(routeVersionId);
    }

    /**
     * 在草稿版本上新增步骤。
     *
     * @param routeVersionId Route 版本
     * @param request 步骤内容
     * @throws BusinessException 版本不是草稿，序号冲突，或关联主数据不存在
     */
    @Transactional
    public void create(Long routeVersionId, RouteStepSaveRequest request) {
        requireDraft(routeVersionId);
        if (repository.findBySeq(routeVersionId, request.getSeqNo()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_SEQ_EXISTS, "步骤序号已存在: " + request.getSeqNo());
        }
        RouteStep step = new RouteStep();
        step.setRouteVersionId(routeVersionId);
        copy(request, step);
        repository.save(step);
    }

    /**
     * 修改草稿版本上的步骤。
     *
     * @param routeVersionId Route 版本
     * @param id 步骤主键
     * @param request 步骤内容
     * @throws BusinessException 步骤不存在，版本不是草稿，或序号与其他步骤冲突
     */
    @Transactional
    public void update(Long routeVersionId, Long id, RouteStepSaveRequest request) {
        requireDraft(routeVersionId);
        RouteStep step = require(routeVersionId, id);
        repository.findBySeq(routeVersionId, request.getSeqNo())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_SEQ_EXISTS, "步骤序号已存在: " + request.getSeqNo());
                });
        copy(request, step);
        repository.update(step);
    }

    /**
     * 删除草稿版本上的步骤。
     *
     * @param routeVersionId Route 版本
     * @param id 步骤主键
     * @throws BusinessException 步骤不存在或版本不是草稿
     */
    @Transactional
    public void delete(Long routeVersionId, Long id) {
        requireDraft(routeVersionId);
        require(routeVersionId, id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.ROUTE_STEP_IN_USE, "Route 步骤",
                DeleteReferenceUtils.reference("步骤 Recipe", used));
        repository.deleteById(id);
    }

    private void copy(RouteStepSaveRequest request, RouteStep step) {
        if (stepRepository.findById(request.getStepId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在");
        }
        int convert = request.getIsUnitConvert() == null ? 0 : request.getIsUnitConvert();
        if (convert == 1 && request.getConvertToUnitId() == null) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_CONVERT_UNIT_REQUIRED, "单位转换站点必须指定转换后的计数单位");
        }
        if (request.getQueueTimeAction() != null && !request.getQueueTimeAction().isBlank()
                && !QUEUE_ACTIONS.contains(request.getQueueTimeAction())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_QUEUE_ACTION_INVALID,
                    "Queue Time 动作不合法: " + request.getQueueTimeAction());
        }
        if (request.getCountUnitId() != null && countUnitRepository.findById(request.getCountUnitId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND, "计数单位不存在");
        }
        if (request.getConvertToUnitId() != null && countUnitRepository.findById(request.getConvertToUnitId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND, "计数单位不存在");
        }
        if (request.getEquipmentGroupId() != null && equipmentGroupRepository.findById(request.getEquipmentGroupId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND, "设备组不存在");
        }
        step.setSeqNo(request.getSeqNo());
        step.setStepId(request.getStepId());
        step.setStepAlias(request.getStepAlias());
        step.setIsRequired(request.getIsRequired() == null ? 1 : request.getIsRequired());
        step.setEquipmentGroupId(request.getEquipmentGroupId());
        step.setCountUnitId(request.getCountUnitId());
        step.setIsUnitConvert(convert);
        step.setConvertToUnitId(request.getConvertToUnitId());
        step.setStdProcessMin(request.getStdProcessMin());
        step.setQueueTimeMaxMin(request.getQueueTimeMaxMin());
        step.setQueueTimeWarnMin(request.getQueueTimeWarnMin());
        step.setQueueTimeAction(request.getQueueTimeAction());
    }

    private RouteVersion requireDraft(Long routeVersionId) {
        RouteVersion version = requireVersion(routeVersionId);
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT, "只有草稿版本可以修改步骤");
        }
        return version;
    }

    private RouteVersion requireVersion(Long routeVersionId) {
        return versionRepository.findById(routeVersionId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_FOUND, "Route 版本不存在"));
    }

    private RouteStep require(Long routeVersionId, Long id) {
        RouteStep step = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "Route 步骤不存在"));
        if (!routeVersionId.equals(step.getRouteVersionId())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "Route 步骤不存在");
        }
        return step;
    }
}
