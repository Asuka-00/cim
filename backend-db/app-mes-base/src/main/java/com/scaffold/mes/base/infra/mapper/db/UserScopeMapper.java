package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.UserScope;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UserScopeMapper 用户数据权限 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UserScopeMapper extends BatchBaseMapper<UserScope> {
}
