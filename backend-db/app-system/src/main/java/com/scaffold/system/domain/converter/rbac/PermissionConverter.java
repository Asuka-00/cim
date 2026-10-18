package com.scaffold.system.domain.converter.rbac;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.rbac.PermissionCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.PermissionUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.vo.rbac.PermissionVO;

/**
 * PermissionConverter转换器接口
 * <p>
 * 负责提供PermissionConverter相关对象转换能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionConverter {

    /**
     * 创建请求转换为实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    Permission toEntity(PermissionCreateRequestDTO createRequest);

    /**
     * 更新请求映射到实体
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissionCode", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntity(PermissionUpdateRequestDTO updateRequest, @MappingTarget Permission permission);

    /**
     * Permission 实体转换为 PermissionVO
     */
    PermissionVO toVO(Permission permission);

    /**
     * Permission 实体列表转换为 PermissionVO 列表
     */
    List<PermissionVO> toVOList(List<Permission> permissions);
}
