package com.scaffold.system.domain.converter.system;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.system.TenantCreateRequestDTO;
import com.scaffold.system.domain.dto.system.TenantUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.vo.system.TenantSelectVO;
import com.scaffold.system.domain.vo.system.TenantVO;

/**
 * TenantConverter转换器接口
 * <p>
 * 负责提供TenantConverter相关对象转换能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TenantConverter {

    /**
     * 创建请求转换为实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    Tenant toEntity(TenantCreateRequestDTO createRequest);

    /**
     * 更新请求映射到实体
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntity(TenantUpdateRequestDTO updateRequest, @MappingTarget Tenant tenant);

    /**
     * Tenant 实体转换为 TenantVO
     */
    TenantVO toVO(Tenant tenant);

    /**
     * Tenant 实体列表转换为 TenantVO 列表
     */
    List<TenantVO> toVOList(List<Tenant> tenants);

    /**
     * Tenant 实体转换为下拉选择 VO
     */
    TenantSelectVO toSelectVO(Tenant tenant);

    /**
     * Tenant 实体列表转换为下拉选择 VO 列表
     */
    List<TenantSelectVO> toSelectVOList(List<Tenant> tenants);
}
