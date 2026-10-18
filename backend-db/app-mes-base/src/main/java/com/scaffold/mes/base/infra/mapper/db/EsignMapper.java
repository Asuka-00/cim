package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.Esign;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EsignMapper 电子签名 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EsignMapper extends BatchBaseMapper<Esign> {
}
