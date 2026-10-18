package com.scaffold.system.domain.converter.rbac;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.rbac.UserCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.UserUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.vo.rbac.UserVO;
import com.scaffold.system.domain.vo.rbac.UserDetailVO;

/**
 * UserConverter转换器接口
 * <p>
 * 负责提供UserConverter相关对象转换能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {

    /**
     * 创建请求转换为实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "salt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "platformAdmin", ignore = true)
    User toEntity(UserCreateRequestDTO createRequest);

    /**
     * 更新请求映射到实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "salt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "platformAdmin", ignore = true)
    void updateEntity(UserUpdateRequestDTO updateRequest, @MappingTarget User user);

    /**
     * User 实体转换为 UserDetailVO
     */
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "tenantNames", ignore = true)
    UserDetailVO toDetailView(User user);

    /**
     * 用户实体转换为列表VO（不含角色）
     */
    @Mapping(target = "tenantNames", ignore = true)
    UserVO toVO(User user);

    /**
     * User 实体列表转换为 UserDetailVO 列表
     */
    List<UserDetailVO> toVOList(List<User> users);
}
