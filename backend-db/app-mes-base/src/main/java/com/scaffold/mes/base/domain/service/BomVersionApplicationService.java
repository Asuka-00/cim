package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BomVersionApplicationService BOM 版本应用服务
 * <p>
 * 草稿可改、可提交。通过前先锁 BOM 主表，再把原来的 ACTIVE 降为 INACTIVE。驳回回到草稿。复制新草稿时，BOM 行一并复制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BomVersionApplicationService {

    static final String BIZ_TYPE = "BOM_VERSION";
    private static final String DRAFT = "DRAFT";
    private static final String PENDING = "PENDING";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final BomVersionRepository repository;
    private final BomRepository bomRepository;
    private final BomItemRepository itemRepository;
    private final ApprovalRecordApplicationService approvalService;
    private final ApprovalRecordRepository approvalRepository;
    private final ConfirmGateApplicationService confirms;
    private final Clock clock;

    /**
     * 分页查询某个 BOM 的版本。
     *
     * @param bomId BOM
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageDTO<BomVersion> page(Long bomId, int page, int pageSize) {
        if (bomRepository.findById(bomId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.BOM_NOT_FOUND, "BOM 不存在");
        }
        return repository.page(bomId, page, pageSize);
    }

    /**
     * 新增草稿。
     *
     * @param bomId BOM
     * @param request 备注
     */
    @Transactional
    public void create(Long bomId, RouteVersionSaveRequest request) {
        bomRepository.lock(bomId);
        BomVersion version = new BomVersion();
        version.setBomId(bomId);
        version.setVersionNo(repository.nextVersionNo(bomId));
        version.setStatus(DRAFT);
        version.setRemark(request.getRemark());
        repository.save(version);
    }

    /**
     * 修改草稿备注。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 备注
     * @throws BusinessException 版本不存在或不是草稿
     */
    @Transactional
    public void update(Long bomId, Long id, RouteVersionSaveRequest request) {
        BomVersion version = requireDraft(bomId, id);
        version.setRemark(request.getRemark());
        repository.update(version);
    }

    /**
     * 复制成新草稿，并带上全部 BOM 行。
     *
     * @param bomId BOM
     * @param id 来源版本
     * @throws BusinessException 来源版本不存在
     */
    @Transactional
    public void copy(Long bomId, Long id) {
        bomRepository.lock(bomId);
        BomVersion source = require(bomId, id);
        BomVersion copy = new BomVersion();
        copy.setBomId(bomId);
        copy.setVersionNo(repository.nextVersionNo(bomId));
        copy.setStatus(DRAFT);
        copy.setRemark(source.getRemark());
        copy.setCopyFromVersionId(source.getId());
        repository.save(copy);
        for (BomItem item : itemRepository.listByVersion(source.getId())) {
            itemRepository.save(copyItem(item, copy.getId()));
        }
    }

    /**
     * 提交审批。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param operator 提交人
     * @param request 审批人和意见
     * @throws BusinessException 不是草稿，或没指定审批人
     */
    @Transactional
    public void submit(Long bomId, Long id, String operator, RouteVersionSubmitRequest request) {
        BomVersion version = requireDraft(bomId, id);
        if (request.getAssignee() == null || request.getAssignee().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_ASSIGNEE_REQUIRED, "提交审批必须指定审批人");
        }
        version.setStatus(PENDING);
        version.setSubmittedBy(operator);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "SUBMIT", DRAFT, PENDING, request.getAssignee(), operator, request.getComment());
    }

    /**
     * 审批通过并生效。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void approve(Long bomId, Long id, RouteVersionDecisionRequest request) {
        if (!confirms.allowed("VERSION_ACTIVATE", "BOM_VERSION", id)) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_REQUIRED, "版本生效还没有完成确认");
        }
        bomRepository.lock(bomId);
        BomVersion version = requirePending(bomId, id);
        requireAssignee(id, request.getOperator());
        LocalDateTime now = LocalDateTime.now(clock);
        for (BomVersion active : repository.listActive(bomId)) {
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
    }

    /**
     * 驳回为草稿。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void reject(Long bomId, Long id, RouteVersionDecisionRequest request) {
        BomVersion version = requirePending(bomId, id);
        requireAssignee(id, request.getOperator());
        version.setStatus(DRAFT);
        version.setSubmittedBy(null);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "REJECT", PENDING, DRAFT, null, request.getOperator(), request.getComment());
    }

    private static BomItem copyItem(BomItem source, Long bomVersionId) {
        BomItem copy = new BomItem();
        copy.setBomVersionId(bomVersionId);
        copy.setLineNo(source.getLineNo());
        copy.setMaterialId(source.getMaterialId());
        copy.setQtyPer(source.getQtyPer());
        copy.setUomId(source.getUomId());
        copy.setScrapRate(source.getScrapRate());
        copy.setConsumeMode(source.getConsumeMode());
        copy.setStepId(source.getStepId());
        copy.setRouteSeqNo(source.getRouteSeqNo());
        copy.setBackflush(source.getBackflush());
        copy.setIsCritical(source.getIsCritical());
        copy.setSubstituteGroup(source.getSubstituteGroup());
        copy.setSubstitutePriority(source.getSubstitutePriority());
        copy.setSubstitutePolicy(source.getSubstitutePolicy());
        copy.setRemark(source.getRemark());
        return copy;
    }

    private void requireAssignee(Long versionId, String operator) {
        ApprovalRecord submit = approvalRepository.findLatest(BIZ_TYPE, versionId, "SUBMIT")
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_PENDING, "没有待审批的提交记录"));
        if (submit.getAssignee() == null || !submit.getAssignee().equals(operator)) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE, "只有指定审批人可以通过或驳回");
        }
    }

    private BomVersion requireDraft(Long bomId, Long id) {
        BomVersion version = require(bomId, id);
        if (!DRAFT.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_DRAFT, "只有草稿可以修改或提交");
        }
        return version;
    }

    private BomVersion requirePending(Long bomId, Long id) {
        BomVersion version = require(bomId, id);
        if (!PENDING.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_PENDING, "只有待审批版本可以审批");
        }
        return version;
    }

    private BomVersion require(Long bomId, Long id) {
        BomVersion version = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_FOUND, "BOM 版本不存在"));
        if (!bomId.equals(version.getBomId())) {
            throw new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_FOUND, "BOM 版本不存在");
        }
        return version;
    }
}
