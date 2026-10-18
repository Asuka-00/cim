package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RecipeVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.RecipeVersionRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RecipeVersionApplicationService Recipe 版本应用服务
 * <p>
 * 草稿可改、可提交。通过前先锁 Recipe 主表，再把原来的 ACTIVE 降为 INACTIVE。驳回回到草稿。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RecipeVersionApplicationService {

    static final String BIZ_TYPE = "RECIPE_VERSION";
    private static final String DRAFT = "DRAFT";
    private static final String PENDING = "PENDING";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final RecipeVersionRepository repository;
    private final RecipeRepository recipeRepository;
    private final ApprovalRecordApplicationService approvalService;
    private final ApprovalRecordRepository approvalRepository;
    private final ConfirmGateApplicationService confirms;
    private final Clock clock;

    /**
     * 分页查询某个 Recipe 的版本。
     *
     * @param recipeId Recipe
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageDTO<RecipeVersion> page(Long recipeId, int page, int pageSize) {
        requireRecipe(recipeId);
        return repository.page(recipeId, page, pageSize);
    }

    /**
     * 新增草稿。版本号在该 Recipe 下递增。
     *
     * @param recipeId Recipe
     * @param request 参数说明
     * @throws BusinessException Recipe 不存在
     */
    @Transactional
    public void create(Long recipeId, RecipeVersionSaveRequest request) {
        recipeRepository.lock(recipeId);
        RecipeVersion version = new RecipeVersion();
        version.setRecipeId(recipeId);
        version.setVersionNo(repository.nextVersionNo(recipeId));
        version.setStatus(DRAFT);
        version.setParamDesc(request.getParamDesc());
        version.setRemark(request.getRemark());
        repository.save(version);
    }

    /**
     * 修改草稿内容。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 参数说明
     * @throws BusinessException 版本不存在，或不是草稿
     */
    @Transactional
    public void update(Long recipeId, Long id, RecipeVersionSaveRequest request) {
        RecipeVersion version = requireDraft(recipeId, id);
        version.setParamDesc(request.getParamDesc());
        version.setRemark(request.getRemark());
        repository.update(version);
    }

    /**
     * 按现有版本复制出下一个草稿。
     *
     * @param recipeId Recipe
     * @param id 来源版本
     * @throws BusinessException 来源版本不存在
     */
    @Transactional
    public void copy(Long recipeId, Long id) {
        recipeRepository.lock(recipeId);
        RecipeVersion source = require(recipeId, id);
        RecipeVersion copy = new RecipeVersion();
        copy.setRecipeId(recipeId);
        copy.setVersionNo(repository.nextVersionNo(recipeId));
        copy.setStatus(DRAFT);
        copy.setParamDesc(source.getParamDesc());
        copy.setRemark(source.getRemark());
        repository.save(copy);
    }

    /**
     * 提交审批。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param operator 提交人
     * @param request 审批人和意见
     * @throws BusinessException 不是草稿，或没指定审批人
     */
    @Transactional
    public void submit(Long recipeId, Long id, String operator, RouteVersionSubmitRequest request) {
        RecipeVersion version = requireDraft(recipeId, id);
        if (request.getAssignee() == null || request.getAssignee().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_ASSIGNEE_REQUIRED, "提交审批必须指定审批人");
        }
        version.setStatus(PENDING);
        version.setSubmittedBy(operator);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "SUBMIT", DRAFT, PENDING, request.getAssignee(), operator, request.getComment());
    }

    /**
     * 审批通过并生效。同一 Recipe 上原 ACTIVE 同时失效。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void approve(Long recipeId, Long id, RouteVersionDecisionRequest request) {
        if (!confirms.allowed("VERSION_ACTIVATE", "RECIPE_VERSION", id)) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_REQUIRED, "版本生效还没有完成确认");
        }
        recipeRepository.lock(recipeId);
        RecipeVersion version = requirePending(recipeId, id);
        requireAssignee(id, request.getOperator());
        LocalDateTime now = LocalDateTime.now(clock);
        for (RecipeVersion active : repository.listActive(recipeId)) {
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
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void reject(Long recipeId, Long id, RouteVersionDecisionRequest request) {
        RecipeVersion version = requirePending(recipeId, id);
        requireAssignee(id, request.getOperator());
        version.setStatus(DRAFT);
        version.setSubmittedBy(null);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "REJECT", PENDING, DRAFT, null, request.getOperator(), request.getComment());
    }

    private void requireAssignee(Long versionId, String operator) {
        ApprovalRecord submit = approvalRepository.findLatest(BIZ_TYPE, versionId, "SUBMIT")
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.RECIPE_VERSION_NOT_PENDING, "没有待审批的提交记录"));
        if (submit.getAssignee() == null || !submit.getAssignee().equals(operator)) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE, "只有指定审批人可以通过或驳回");
        }
    }

    private RecipeVersion requirePending(Long recipeId, Long id) {
        RecipeVersion version = require(recipeId, id);
        if (!PENDING.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_VERSION_NOT_PENDING, "只有待审批版本可以审批");
        }
        return version;
    }

    /**
     * 删除草稿。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @throws BusinessException 版本不存在，或不是草稿
     */
    @Transactional
    public void delete(Long recipeId, Long id) {
        RecipeVersion version = requireDraft(recipeId, id);
        repository.deleteById(version.getId());
    }

    private RecipeVersion requireDraft(Long recipeId, Long id) {
        RecipeVersion version = require(recipeId, id);
        if (!DRAFT.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_VERSION_LOCKED, "已生效或已失效的版本不能修改");
        }
        return version;
    }

    private void requireRecipe(Long recipeId) {
        if (recipeRepository.findById(recipeId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在");
        }
    }

    private RecipeVersion require(Long recipeId, Long id) {
        RecipeVersion version = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.RECIPE_VERSION_NOT_FOUND, "Recipe 版本不存在"));
        if (!recipeId.equals(version.getRecipeId())) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_VERSION_NOT_FOUND, "Recipe 版本不存在");
        }
        return version;
    }
}
