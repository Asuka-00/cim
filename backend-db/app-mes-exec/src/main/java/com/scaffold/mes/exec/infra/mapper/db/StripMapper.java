package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.Strip;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StripMapper Strip Mapper
 * <p>
 * 映射 mes_strip。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StripMapper extends BatchBaseMapper<Strip> {
}
