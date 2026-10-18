package com.scaffold.system.domain.converter.rbac;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.rbac.RoleCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.RoleUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.vo.rbac.RoleVO;

/**
 * RoleConverter转换器接口
 * <p>
 * 负责提供RoleConverter相关对象转换能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleConverter {

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
    Role toEntity(RoleCreateRequestDTO createRequest);

    /**
     * 更新请求映射到实体
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roleCode", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntity(RoleUpdateRequestDTO updateRequest, @MappingTarget Role role);

    /**
     * Role 实体转换为 RoleVO
     */
    RoleVO toVO(Role role);

    /**
     * Role 实体列表转换为 RoleVO 列表
     */
    List<RoleVO> toVOList(List<Role> roles);
}
