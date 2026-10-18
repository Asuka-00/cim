package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;

/**
 * ApprovalRecordRepository 审批记录仓储
 * <p>
 * 只追加 hist_mes_approval_record。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ApprovalRecordRepository {

    /**
     * 追加一条审批记录。
     *
     * @param record 记录
     */
    void save(ApprovalRecord record);

    /**
     * 按业务对象分页查询，动作时间从新到旧。
     *
     * @param bizType 业务类型
     * @param bizId 业务对象主键
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageDTO<ApprovalRecord> page(String bizType, Long bizId, int page, int pageSize);

    /**
     * 取该对象最近一条指定动作的记录。
     *
     * @param bizType 业务类型
     * @param bizId 业务对象主键
     * @param action 动作
     * @return 记录，没有时为空
     */
    Optional<ApprovalRecord> findLatest(String bizType, Long bizId, String action);

    /**
     * 列出指定审批人的提交记录，主键从新到旧。
     *
     * @param assignee 指定审批人
     * @return 提交记录
     */
    List<ApprovalRecord> listSubmits(String assignee);
}
