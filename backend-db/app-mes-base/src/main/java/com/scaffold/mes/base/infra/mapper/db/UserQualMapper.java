package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.UserQual;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UserQualMapper 人员资质 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UserQualMapper extends BatchBaseMapper<UserQual> {
}
