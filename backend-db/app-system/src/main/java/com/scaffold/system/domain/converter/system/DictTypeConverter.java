package com.scaffold.system.domain.converter.system;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.system.DictTypeCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictTypeUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.DictType;
import com.scaffold.system.domain.vo.system.DictTypeVO;

/**
 * DictTypeConverter 转换器接口
 * <p>
 * 负责字典类型请求、实体和视图对象之间的转换。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictTypeConverter {

    /**
     * 创建请求转换为字典类型实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    DictType toEntity(DictTypeCreateRequestDTO request);

    /**
     * 更新请求映射到字典类型实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "dictType", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntity(DictTypeUpdateRequestDTO request, @MappingTarget DictType entity);

    /**
     * 字典类型实体转换为视图对象
     */
    DictTypeVO toVO(DictType entity);

    /**
     * 字典类型实体列表转换为视图对象列表
     */
    List<DictTypeVO> toVOList(List<DictType> entities);
}
