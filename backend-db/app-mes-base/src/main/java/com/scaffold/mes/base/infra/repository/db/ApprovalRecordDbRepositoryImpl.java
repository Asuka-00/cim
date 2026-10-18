package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.infra.mapper.db.ApprovalRecordMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ApprovalRecordDbRepositoryImpl 审批记录仓储实现
 * <p>
 * 只插入和查询，不提供更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ApprovalRecordDbRepositoryImpl implements ApprovalRecordRepository {

    private final ApprovalRecordMapper mapper;

    private AppendOnlyRepositorySupport<ApprovalRecord> support() {
        return new AppendOnlyRepositorySupport<>(mapper, ApprovalRecord.class);
    }

    @Override
    public void save(ApprovalRecord record) {
        support().save(record);
    }

    @Override
    public PageDTO<ApprovalRecord> page(String bizType, Long bizId, int page, int pageSize) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getBizType, bizType)
                .eq(ApprovalRecord::getBizId, bizId)
                .orderByDesc(ApprovalRecord::getActionAt);
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public Optional<ApprovalRecord> findLatest(String bizType, Long bizId, String action) {
        return support().findOne(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getBizType, bizType)
                .eq(ApprovalRecord::getBizId, bizId)
                .eq(ApprovalRecord::getAction, action)
                .orderByDesc(ApprovalRecord::getActionAt));
    }

    @Override
    public List<ApprovalRecord> listSubmits(String assignee) {
        return mapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAction, "SUBMIT")
                .eq(ApprovalRecord::getAssignee, assignee)
                .orderByDesc(ApprovalRecord::getId));
    }
}
