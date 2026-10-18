package com.scaffold.system.infra.mapper.db.system;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.system.SystemConfig;

/**
 * SystemConfigMapper映射接口
 * <p>
 * 负责定义SystemConfigMapper相关数据库映射操作
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper
public interface SystemConfigMapper extends BatchBaseMapper<SystemConfig> {

    /**
     * 初始化专用：忽略租户拦截，全量分页查询
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_system_config WHERE deleted_at IS NULL")
    IPage<SystemConfig> selectInitPageIgnoreTenant(IPage<SystemConfig> page);

    /**
     * 按分组查询（忽略租户）：OAuth2 等登录前实例级配置
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_system_config WHERE deleted_at IS NULL AND group_code = #{groupCode}")
    List<SystemConfig> selectByGroupCodeIgnoreTenant(@Param("groupCode") String groupCode);
}
