package com.scaffold.system.domain.converter.rbac;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.vo.rbac.LoginVO;

/**
 * LoginConverter转换器接口
 * <p>
 * 负责统一组装登录/刷新令牌返回体，避免在服务层重复搬运字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-21
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LoginConverter {

    /**
     * 组装登录返回体
     */
    @Mapping(target = "userName", source = "user.userName")
    @Mapping(target = "displayName", source = "user.displayName")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "accessExpireSeconds", source = "accessExpireSeconds")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenants", ignore = true)
    @Mapping(target = "platformAdmin", source = "user.platformAdmin")
    LoginVO toLoginVO(User user, String accessToken, String refreshToken, Long accessExpireSeconds);
}
