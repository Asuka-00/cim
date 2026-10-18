package com.scaffold.system.infra.mapper.db.rbac;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.rbac.User;

/**
 * UserMapper映射接口
 * <p>
 * 负责定义UserMapper相关数据库映射操作
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper
public interface UserMapper extends BatchBaseMapper<User> {

    /**
     * 初始化专用：忽略租户拦截，全量分页查询
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE deleted_at IS NULL")
    IPage<User> selectInitPageIgnoreTenant(IPage<User> page);

    /**
     * 根据用户账号和租户ID查询用户（登录专用，跳过租户拦截器）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT * FROM sys_user WHERE user_name = #{userName} AND deleted_at IS NULL"
            + "<if test='tenantId != null and tenantId != \"\"'> AND tenant_id = #{tenantId}</if>"
            + "</script>")
    User selectByUserNameWithTenant(@Param("userName") String userName, @Param("tenantId") String tenantId);
}
