package com.scaffold.system.infra.mapper.db.rbac;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.rbac.Permission;

/**
 * PermissionMapper映射接口
 * <p>
 * 负责定义PermissionMapper相关数据库映射操作
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper
public interface PermissionMapper extends BatchBaseMapper<Permission> {

        /**
         * 初始化专用：忽略租户拦截，全量分页查询
         */
        @InterceptorIgnore(tenantLine = "true")
        @Select("SELECT * FROM sys_permission WHERE deleted_at IS NULL")
        IPage<Permission> selectInitPageIgnoreTenant(IPage<Permission> page);

        /**
         * 根据角色ID查询权限列表
         */
        @Select("SELECT p.* FROM sys_permission p " +
                        "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
                        "WHERE rp.role_id = #{roleId} AND p.deleted_at IS NULL")
        List<Permission> selectByRoleId(Long roleId);

        /**
         * 根据用户ID查询权限列表
         */
        @Select("SELECT p.* FROM sys_permission p " +
                        "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
                        "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = #{userId} AND p.deleted_at IS NULL")
        List<Permission> selectByUserId(Long userId);
}
