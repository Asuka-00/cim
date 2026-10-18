package com.scaffold.system.infra.mapper.db.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.rbac.UserTenant;

/**
 * UserTenantMapper 用户-租户成员资格映射
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Mapper
public interface UserTenantMapper extends BatchBaseMapper<UserTenant> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user_tenant")
    IPage<UserTenant> selectInitPageIgnoreTenant(IPage<UserTenant> page);
}
