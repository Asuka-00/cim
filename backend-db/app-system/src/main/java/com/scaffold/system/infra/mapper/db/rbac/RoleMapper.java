package com.scaffold.system.infra.mapper.db.rbac;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.rbac.Role;

/**
 * RoleMapper映射接口
 * <p>
 * 负责定义RoleMapper相关数据库映射操作
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper
public interface RoleMapper extends BatchBaseMapper<Role> {

    /**
     * 初始化专用：忽略租户拦截，全量分页查询
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_role WHERE deleted_at IS NULL")
    IPage<Role> selectInitPageIgnoreTenant(IPage<Role> page);

    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted_at IS NULL")
    List<Role> selectByUserId(Long userId);
}
