package com.scaffold.mes.core.infra.mapper.db.uda;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UdaDefMapper 自定义属性定义 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UdaDefMapper extends BatchBaseMapper<UdaDef> {
}
