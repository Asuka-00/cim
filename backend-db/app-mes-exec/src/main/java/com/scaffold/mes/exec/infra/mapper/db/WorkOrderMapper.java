package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * WorkOrderMapper 工单 Mapper
 * <p>
 * 映射 mes_work_order。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface WorkOrderMapper extends BatchBaseMapper<WorkOrder> {
}
