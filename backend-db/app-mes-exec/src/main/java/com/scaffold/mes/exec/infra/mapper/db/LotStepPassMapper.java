package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotStepPassMapper 站点经过次数 Mapper
 * <p>
 * 映射 mes_lot_step_pass。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotStepPassMapper extends BatchBaseMapper<LotStepPass> {
}
