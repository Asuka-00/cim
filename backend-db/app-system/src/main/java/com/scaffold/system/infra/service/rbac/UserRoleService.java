package com.scaffold.system.infra.service.rbac;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.infra.mapper.db.rbac.UserRoleMapper;

/**
 * UserRoleService服务类
 * <p>
 * 提供UserRole批量操作能力，基于MyBatis-Plus IService实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
public class UserRoleService extends ServiceImpl<UserRoleMapper, UserRole> {
}
