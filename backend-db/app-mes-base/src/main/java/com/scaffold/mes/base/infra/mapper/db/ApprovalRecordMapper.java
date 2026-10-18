package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ApprovalRecordMapper 审批记录 Mapper
 * <p>
 * 映射 hist_mes_approval_record。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ApprovalRecordMapper extends BatchBaseMapper<ApprovalRecord> {
}
