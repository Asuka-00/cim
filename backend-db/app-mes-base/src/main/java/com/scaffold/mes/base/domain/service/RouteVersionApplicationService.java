package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.core.domain.service.alert.MesAlertApplicationService;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.entity.ProductVersion;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.domain.repository.ProductVersionRepository;
import com.scaffold.mes.base.domain.repository.RouteRepository;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RouteVersionApplicationService Route 版本应用服务
 * <p>
 * 草稿可改、可提交。通过前先锁 Route 主表，再把原来的 ACTIVE 降为 INACTIVE。驳回回到草稿。
 * 复制新草稿时，步骤、Time Link、步骤 Recipe、步骤抽检覆盖和步骤采集规格一并复制。返工和工具覆盖留到后续里程碑。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteVersionApplicationService {

    static final String BIZ_TYPE = "ROUTE_VERSION";
    private static final String DRAFT = "DRAFT";
    private static final String PENDING = "PENDING";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final RouteVersionRepository repository;
    private final RouteRepository routeRepository;
    private final RouteStepRepository stepRepository;
    private final RouteTimeLinkRepository timeLinkRepository;
    private final RouteStepRecipeRepository stepRecipeRepository;
    private final RouteStepSamplingRepository stepSamplingRepository;
    private final RouteStepCollectItemRepository stepCollectRepository;
    private final StepRepository stationRepository;
    private final StepEquipmentGroupRepository equipmentGroupRepository;
    private final ProductVersionRepository productVersionRepository;
    private final BomVersionRepository bomVersionRepository;
    private final BomItemRepository bomItemRepository;
    private final MesAlertApplicationService alertService;
    private final ApprovalRecordApplicationService approvalService;
    private final ApprovalRecordRepository approvalRepository;
    private final ConfirmGateApplicationService confirms;
    private final Clock clock;

    /**
     * 分页查询某个 Route 的版本。
     *
     * @param routeId Route
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageDTO<RouteVersion> page(Long routeId, int page, int pageSize) {
        requireRoute(routeId);
        return repository.page(routeId, page, pageSize);
    }

    /**
     * 新增草稿。
     *
     * @param routeId Route
     * @param request 备注
     */
    @Transactional
    public void create(Long routeId, RouteVersionSaveRequest request) {
        routeRepository.lock(routeId);
        RouteVersion version = new RouteVersion();
        version.setRouteId(routeId);
        version.setVersionNo(repository.nextVersionNo(routeId));
        version.setStatus(DRAFT);
        version.setRemark(request.getRemark());
        repository.save(version);
    }

    /**
     * 修改草稿备注。
     *
     * @param routeId Route
     * @param id 版本主键
     * @param request 备注
     * @throws BusinessException 版本不存在或不是草稿
     */
    @Transactional
    public void update(Long routeId, Long id, RouteVersionSaveRequest request) {
        RouteVersion version = requireDraft(routeId, id);
        version.setRemark(request.getRemark());
        repository.update(version);
    }

    /**
     * 复制成新草稿，并带上步骤、Time Link、步骤 Recipe、抽检覆盖和采集规格。
     * <p>
     * 步骤 Recipe 改挂到新步骤主键上。序号不变，所以 Time Link 不用改序号。
     *
     * @param routeId Route
     * @param id 来源版本
     * @throws BusinessException 来源版本不存在，或步骤 Recipe 找不到对应步骤
     */
    @Transactional
    public void copy(Long routeId, Long id) {
        routeRepository.lock(routeId);
        RouteVersion source = require(routeId, id);
        RouteVersion copy = new RouteVersion();
        copy.setRouteId(routeId);
        copy.setVersionNo(repository.nextVersionNo(routeId));
        copy.setStatus(DRAFT);
        copy.setRemark(source.getRemark());
        copy.setCopyFromVersionId(source.getId());
        repository.save(copy);
        Map<Long, Long> stepIds = copySteps(source.getId(), copy.getId());
        copyTimeLinks(source.getId(), copy.getId());
        copyStepRecipes(source.getId(), copy.getId(), stepIds);
        copyStepSamplings(source.getId(), copy.getId(), stepIds);
        copyStepCollects(source.getId(), copy.getId(), stepIds);
    }

    /**
     * 提交审批。草稿变为 PENDING。
     *
     * @param routeId Route
     * @param id 版本主键
     * @param operator 提交人
     * @param request 审批人和意见
     * @throws BusinessException 不是草稿，没指定审批人，或需要设备的步骤没有设备组
     */
    @Transactional
    public void submit(Long routeId, Long id, String operator, RouteVersionSubmitRequest request) {
        RouteVersion version = requireDraft(routeId, id);
        requireEquipmentGroups(id);
        if (request.getAssignee() == null || request.getAssignee().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_ASSIGNEE_REQUIRED, "提交审批必须指定审批人");
        }
        version.setStatus(PENDING);
        version.setSubmittedBy(operator);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "SUBMIT", DRAFT, PENDING, request.getAssignee(), operator, request.getComment());
    }

    /**
     * 审批通过并生效。同一 Route 上原 ACTIVE 同时失效。引用该 Route 的产品若 BOM 对不上，只写系统预警。
     *
     * @param routeId Route
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void approve(Long routeId, Long id, RouteVersionDecisionRequest request) {
        if (!confirms.allowed("VERSION_ACTIVATE", "ROUTE_VERSION", id)) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_REQUIRED, "版本生效还没有完成确认");
        }
        routeRepository.lock(routeId);
        RouteVersion version = requirePending(routeId, id);
        requireAssignee(id, request.getOperator());
        LocalDateTime now = LocalDateTime.now(clock);
        for (RouteVersion active : repository.listActive(routeId)) {
            active.setStatus(INACTIVE);
            active.setExpireAt(now);
            repository.update(active);
        }
        version.setStatus(ACTIVE);
        version.setEffectiveAt(now);
        version.setExpireAt(null);
        version.setApprovedBy(request.getOperator());
        version.setApprovedAt(now);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "APPROVE", PENDING, ACTIVE, null, request.getOperator(), request.getComment());
        warnMismatchedProducts(version);
    }

    /**
     * 新 Route 已生效后再预检。冲突不回滚生效，只给每个对不上的产品版本写一条预警。
     */
    private void warnMismatchedProducts(RouteVersion routeVersion) {
        List<RouteStep> steps = stepRepository.listByVersion(routeVersion.getId());
        for (ProductVersion productVersion : productVersionRepository.listActiveByRoute(routeVersion.getRouteId())) {
            if (productVersion.getBomId() == null) {
                continue;
            }
            List<BomVersion> boms = bomVersionRepository.listActive(productVersion.getBomId());
            if (boms.isEmpty()) {
                continue;
            }
            List<BomItem> items = bomItemRepository.listByVersion(boms.get(0).getId());
            Optional<Integer> line = BomRouteAlignment.firstMismatch(steps, items);
            if (line.isEmpty()) {
                continue;
            }
            alertService.raiseSystem("PRODUCT_VERSION", productVersion.getId(), "Route 与 BOM 不一致",
                    "产品版本 " + productVersion.getId() + " 的 BOM 行 " + line.get() + " 与新 Route 版本 " + routeVersion.getId() + " 不一致");
        }
    }

    /**
     * 驳回。待审批版本回到草稿。
     *
     * @param routeId Route
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void reject(Long routeId, Long id, RouteVersionDecisionRequest request) {
        RouteVersion version = requirePending(routeId, id);
        requireAssignee(id, request.getOperator());
        version.setStatus(DRAFT);
        version.setSubmittedBy(null);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "REJECT", PENDING, DRAFT, null, request.getOperator(), request.getComment());
    }

    private Map<Long, Long> copySteps(Long sourceVersionId, Long targetVersionId) {
        Map<Long, Long> stepIds = new HashMap<>();
        for (RouteStep source : stepRepository.listByVersion(sourceVersionId)) {
            RouteStep copy = new RouteStep();
            copy.setRouteVersionId(targetVersionId);
            copy.setSeqNo(source.getSeqNo());
            copy.setStepId(source.getStepId());
            copy.setStepAlias(source.getStepAlias());
            copy.setIsRequired(source.getIsRequired());
            copy.setEquipmentGroupId(source.getEquipmentGroupId());
            copy.setCountUnitId(source.getCountUnitId());
            copy.setIsUnitConvert(source.getIsUnitConvert());
            copy.setConvertToUnitId(source.getConvertToUnitId());
            copy.setStdProcessMin(source.getStdProcessMin());
            copy.setQueueTimeMaxMin(source.getQueueTimeMaxMin());
            copy.setQueueTimeWarnMin(source.getQueueTimeWarnMin());
            copy.setQueueTimeAction(source.getQueueTimeAction());
            copy.setIsSamplingStep(source.getIsSamplingStep());
            stepRepository.save(copy);
            stepIds.put(source.getId(), copy.getId());
        }
        return stepIds;
    }

    private void copyTimeLinks(Long sourceVersionId, Long targetVersionId) {
        for (RouteTimeLink source : timeLinkRepository.listByVersion(sourceVersionId)) {
            RouteTimeLink copy = new RouteTimeLink();
            copy.setRouteVersionId(targetVersionId);
            copy.setFromSeqNo(source.getFromSeqNo());
            copy.setToSeqNo(source.getToSeqNo());
            copy.setMaxMinutes(source.getMaxMinutes());
            copy.setWarnMinutes(source.getWarnMinutes());
            copy.setAction(source.getAction());
            copy.setReasonCode(source.getReasonCode());
            timeLinkRepository.save(copy);
        }
    }

    private void copyStepRecipes(Long sourceVersionId, Long targetVersionId, Map<Long, Long> stepIds) {
        for (RouteStepRecipe source : stepRecipeRepository.listByVersion(sourceVersionId)) {
            Long newStepId = stepIds.get(source.getRouteStepId());
            if (newStepId == null) {
                throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "步骤 Recipe 找不到对应步骤");
            }
            RouteStepRecipe copy = new RouteStepRecipe();
            copy.setRouteVersionId(targetVersionId);
            copy.setRouteStepId(newStepId);
            copy.setRecipeId(source.getRecipeId());
            copy.setProductId(source.getProductId());
            copy.setIsDefault(source.getIsDefault());
            stepRecipeRepository.save(copy);
        }
    }

    private void copyStepSamplings(Long sourceVersionId, Long targetVersionId, Map<Long, Long> stepIds) {
        for (RouteStepSampling source : stepSamplingRepository.listByVersion(sourceVersionId)) {
            Long newStepId = stepIds.get(source.getRouteStepId());
            if (newStepId == null) {
                throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "步骤抽检覆盖找不到对应步骤");
            }
            RouteStepSampling copy = new RouteStepSampling();
            copy.setRouteVersionId(targetVersionId);
            copy.setRouteStepId(newStepId);
            copy.setProductId(source.getProductId());
            copy.setRuleId(source.getRuleId());
            stepSamplingRepository.save(copy);
        }
    }

    private void copyStepCollects(Long sourceVersionId, Long targetVersionId, Map<Long, Long> stepIds) {
        for (RouteStepCollectItem source : stepCollectRepository.listByVersion(sourceVersionId)) {
            Long newStepId = stepIds.get(source.getRouteStepId());
            if (newStepId == null) {
                throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "步骤采集规格找不到对应步骤");
            }
            RouteStepCollectItem copy = new RouteStepCollectItem();
            copy.setRouteVersionId(targetVersionId);
            copy.setRouteStepId(newStepId);
            copy.setItemId(source.getItemId());
            copy.setProductId(source.getProductId());
            copy.setLsl(source.getLsl());
            copy.setUsl(source.getUsl());
            copy.setAutoHoldOnOos(source.getAutoHoldOnOos());
            copy.setHoldReasonCode(source.getHoldReasonCode());
            copy.setIsRequired(source.getIsRequired());
            stepCollectRepository.save(copy);
        }
    }

    /**
     * need_equipment=1 的站点必须有步骤指定设备组，或站点级设备组绑定。need_equipment=0 不检查。
     */
    private void requireEquipmentGroups(Long routeVersionId) {
        for (RouteStep routeStep : stepRepository.listByVersion(routeVersionId)) {
            Step station = stationRepository.findById(routeStep.getStepId())
                    .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在"));
            if (station.getNeedEquipment() == null || station.getNeedEquipment() == 0) {
                continue;
            }
            if (routeStep.getEquipmentGroupId() != null || !equipmentGroupRepository.listByStep(station.getId()).isEmpty()) {
                continue;
            }
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_EQUIPMENT_REQUIRED,
                    "步骤需要设备但未配置设备组，序号 " + routeStep.getSeqNo());
        }
    }

    private void requireAssignee(Long versionId, String operator) {
        ApprovalRecord submit = approvalRepository.findLatest(BIZ_TYPE, versionId, "SUBMIT")
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_PENDING, "没有待审批的提交记录"));
        if (submit.getAssignee() == null || !submit.getAssignee().equals(operator)) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE, "只有指定审批人可以通过或驳回");
        }
    }

    private RouteVersion requireDraft(Long routeId, Long id) {
        RouteVersion version = require(routeId, id);
        if (!DRAFT.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT, "只有草稿可以修改或提交");
        }
        return version;
    }

    private RouteVersion requirePending(Long routeId, Long id) {
        RouteVersion version = require(routeId, id);
        if (!PENDING.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_PENDING, "只有待审批版本可以审批");
        }
        return version;
    }

    private void requireRoute(Long routeId) {
        if (routeRepository.findById(routeId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_NOT_FOUND, "Route 不存在");
        }
    }

    private RouteVersion require(Long routeId, Long id) {
        RouteVersion version = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_FOUND, "Route 版本不存在"));
        if (!routeId.equals(version.getRouteId())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_FOUND, "Route 版本不存在");
        }
        return version;
    }
}
