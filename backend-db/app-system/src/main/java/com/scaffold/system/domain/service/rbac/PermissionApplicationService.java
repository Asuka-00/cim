package com.scaffold.system.domain.service.rbac;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.context.SessionContextUtils;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;
import com.scaffold.share.utils.PageQueryUtils;
import com.scaffold.system.domain.converter.rbac.PermissionConverter;
import com.scaffold.system.domain.dto.rbac.PermissionCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.PermissionUpdateRequestDTO;
import com.scaffold.system.domain.dto.rbac.SseNotificationDTO;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.query.rbac.PermissionQuery;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.vo.rbac.PermissionVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PermissionApplicationService服务类
 * <p>
 * 负责处理PermissionApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionApplicationService {

        private final PermissionRepository permissionRepository;
        private final RolePermissionRepository rolePermissionRepository;
        private final PermissionConverter permissionConverter;
        private final SseService sseService;
        private final UserRepository userRepository;
        private final CurrentSessionProvider currentSessionProvider;

        /**
         * 添加权限
         */
        @Transactional
        public void addPermission(PermissionCreateRequestDTO createRequest) {
                requirePlatformAdmin();
                // 检查权限编码是否已存在
                if (permissionRepository.findByPermissionCode(createRequest.getPermissionCode()).isPresent()) {
                        throw new BusinessException(ErrorCodeConstants.PERMISSION_CODE_EXISTS,
                                        "权限编码已存在: " + createRequest.getPermissionCode());
                }

                // 创建权限实体
                Permission permission = permissionConverter.toEntity(createRequest);

                // 保存权限
                permissionRepository.save(permission);

                // 权限树发生变化后，所有在线用户的前端路由/按钮权限理论上都可能受影响
                notifyOnlineUsers("权限[" + createRequest.getPermissionCode() + "]已添加");
                log.info("Permission created: permissionCode={}, permissionId={}", permission.getPermissionCode(),
                                permission.getId());
        }

        /**
         * 删除权限
         */
        @Transactional
        public void deletePermission(Long id) {
                requirePlatformAdmin();
                Permission permission = permissionRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.PERMISSION_NOT_FOUND,
                                                "权限不存在: " + id));

                DeleteReferenceUtils.requireNoReferences(ErrorCodeConstants.PERMISSION_HAS_BEEN_BOUND, "权限",
                                DeleteReferenceUtils.reference("角色权限绑定",
                                                rolePermissionRepository.findByPermissionId(id).size()),
                                DeleteReferenceUtils.reference("子权限", permissionRepository.findByParentId(id).size()));

                permissionRepository.deleteById(permission.getId());

                // 通知所有用户权限已变更
                notifyOnlineUsers("权限[" + permission.getPermissionCode() + "]已删除");
                log.info("Permission deleted: permissionCode={}, permissionId={}", permission.getPermissionCode(),
                                permission.getId());
        }

        /**
         * 更新权限
         */
        @Transactional
        public void updatePermission(Long id, PermissionUpdateRequestDTO updateRequest) {
                requirePlatformAdmin();
                Permission permission = permissionRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.PERMISSION_NOT_FOUND,
                                                "权限不存在: " + id));

                // 更新权限信息
                permissionConverter.updateEntity(updateRequest, permission);

                permissionRepository.update(permission);

                // 更新后统一广播，让客户端自行决定是增量刷新还是重新拉取权限树
                notifyOnlineUsers("权限[" + updateRequest.getPermissionName() + "]已更新");
                log.info("Permission updated: permissionCode={}, permissionId={}", permission.getPermissionCode(),
                                permission.getId());
        }

        /**
         * 分页查询权限
         */
        public PageDTO<PermissionVO> queryPermissionList(int page, int pageSize, PermissionQuery query) {
                // 调用 Repository 分页查询方法
                PageDTO<Permission> permissionPage = permissionRepository.queryPermissionPage(page, pageSize, query);

                return PageQueryUtils.map(permissionPage, permissionConverter::toVO);
        }

        /**
         * 获取权限下拉列表
         */
        public List<PermissionVO> getDropPermissionList(PermissionQuery query) {
                // 下拉数据量通常有限，直接拉取大页足够简单，避免再引入专门的“无分页查询”接口
                PageDTO<Permission> permissionPage = permissionRepository.queryPermissionPage(1, 1000, query);

                return permissionConverter.toVOList(permissionPage.getRecords());
        }

        /**
         * 通知当前在线用户权限已变更
         */
        private void notifyOnlineUsers(String message) {
                sseService.broadcastToAllOnline(SseNotificationDTO.permissionChanged(null, message));
        }

        private void requirePlatformAdmin() {
                String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
                User user = userRepository.findByUserName(userName)
                                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND,
                                                "用户不存在: " + userName));
                PlatformAdminSupport.requirePlatformAdmin(user);
        }
}
