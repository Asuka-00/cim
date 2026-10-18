package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.Handover;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * HandoverMapper 交接班 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface HandoverMapper extends BatchBaseMapper<Handover> {
}
