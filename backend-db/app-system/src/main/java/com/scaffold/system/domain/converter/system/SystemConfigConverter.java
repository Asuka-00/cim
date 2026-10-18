package com.scaffold.system.domain.converter.system;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.system.SystemConfigCreateRequestDTO;
import com.scaffold.system.domain.dto.system.SystemConfigUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.vo.system.SystemConfigVO;

/**
 * SystemConfigConverter转换器接口
 * <p>
 * 负责提供SystemConfigConverter相关对象转换能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SystemConfigConverter {

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
    SystemConfig toEntity(SystemConfigCreateRequestDTO createRequest);

    /**
     * 更新请求映射到实体
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "configCode", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntity(SystemConfigUpdateRequestDTO updateRequest, @MappingTarget SystemConfig systemConfig);

    /**
     * SystemConfig 实体转换为 SystemConfigVO
     */
    SystemConfigVO toVO(SystemConfig systemConfig);

    /**
     * SystemConfig 实体列表转换为 SystemConfigVO 列表
     */
    List<SystemConfigVO> toVOList(List<SystemConfig> systemConfigs);
}
