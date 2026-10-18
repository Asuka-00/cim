package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ApprovalRecordApplicationService 审批记录应用服务
 * <p>
 * 只追加。版本状态怎么变由各版本服务决定，这里只校验类型、动作和操作人。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ApprovalRecordApplicationService {

    private static final Set<String> BIZ_TYPES = Set.of(
            "PRODUCT_VERSION", "ROUTE_VERSION", "BOM_VERSION", "RECIPE_VERSION", "CONSUMPTION_ADJUST", "ECN");
    private static final Set<String> ACTIONS = Set.of("SUBMIT", "APPROVE", "REJECT", "REVOKE");

    private final ApprovalRecordRepository repository;
    private final Clock clock;

    /**
     * 按业务对象查询审批记录。
     *
     * @param bizType 业务类型
     * @param bizId 业务对象主键
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     * @throws BusinessException 业务类型不合法
     */
    public PageDTO<ApprovalRecord> page(String bizType, Long bizId, int page, int pageSize) {
        requireBizType(bizType);
        return repository.page(bizType, bizId, page, pageSize);
    }

    /**
     * 列出仍等这位审批人处理的提交。同一对象只保留最新一次提交，之后已通过或驳回的不再出现。
     *
     * @param assignee 指定审批人
     * @return 待审批记录
     * @throws BusinessException 审批人为空
     */
    public List<ApprovalRecord> listMine(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.APPROVAL_OPERATOR_REQUIRED, "审批人不能为空");
        }
        List<ApprovalRecord> pending = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (ApprovalRecord submit : repository.listSubmits(assignee)) {
            if (!seen.add(submit.getBizType() + "#" + submit.getBizId())) {
                continue;
            }
            if (!decidedAfter(submit)) {
                pending.add(submit);
            }
        }
        return pending;
    }

    private boolean decidedAfter(ApprovalRecord submit) {
        return later(submit, "APPROVE") || later(submit, "REJECT") || later(submit, "REVOKE");
    }

    private boolean later(ApprovalRecord submit, String action) {
        return repository.findLatest(submit.getBizType(), submit.getBizId(), action)
                .map(row -> row.getId() != null && submit.getId() != null && row.getId() > submit.getId())
                .orElse(false);
    }

    /**
     * 追加一条审批动作。
     *
     * @param bizType 业务类型
     * @param bizId 业务对象主键
     * @param action 动作
     * @param fromStatus 变更前状态，可空
     * @param toStatus 变更后状态，可空
     * @param assignee 指定审批人，可空
     * @param operator 操作人
     * @param comment 意见，可空
     * @throws BusinessException 类型、动作或操作人不合法
     */
    @Transactional
    public void record(String bizType, Long bizId, String action, String fromStatus, String toStatus, String assignee,
            String operator, String comment) {
        requireBizType(bizType);
        if (action == null || !ACTIONS.contains(action)) {
            throw new BusinessException(MesBaseErrorCodes.APPROVAL_ACTION_INVALID, "审批动作不合法: " + action);
        }
        if (operator == null || operator.isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.APPROVAL_OPERATOR_REQUIRED, "审批操作人不能为空");
        }
        ApprovalRecord record = new ApprovalRecord();
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setAssignee(assignee);
        record.setOperator(operator);
        record.setComment(comment);
        record.setActionAt(LocalDateTime.now(clock));
        repository.save(record);
    }

    private static void requireBizType(String bizType) {
        if (bizType == null || !BIZ_TYPES.contains(bizType)) {
            throw new BusinessException(MesBaseErrorCodes.APPROVAL_BIZ_TYPE_INVALID, "审批业务类型不合法: " + bizType);
        }
    }
}
