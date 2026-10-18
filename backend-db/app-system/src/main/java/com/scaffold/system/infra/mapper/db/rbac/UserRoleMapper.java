package com.scaffold.system.infra.mapper.db.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.rbac.UserRole;

/**
 * UserRoleMapper映射接口
 * <p>
 * 负责定义UserRoleMapper相关数据库映射操作
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper
public interface UserRoleMapper extends BatchBaseMapper<UserRole> {

    /**
     * 初始化专用：忽略租户拦截，全量分页查询
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user_role")
    IPage<UserRole> selectInitPageIgnoreTenant(IPage<UserRole> page);
}
