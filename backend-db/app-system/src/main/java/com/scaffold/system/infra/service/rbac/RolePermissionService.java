package com.scaffold.system.infra.service.rbac;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.infra.mapper.db.rbac.RolePermissionMapper;

/**
 * RolePermissionService服务类
 * <p>
 * 提供RolePermission批量操作能力，基于MyBatis-Plus IService实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
public class RolePermissionService extends ServiceImpl<RolePermissionMapper, RolePermission> {
}
