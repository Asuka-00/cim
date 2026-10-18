package com.scaffold.system.domain.converter.system;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.scaffold.system.domain.dto.system.DictItemCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictItemUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.DictItem;
import com.scaffold.system.domain.vo.system.DictItemVO;

/**
 * DictItemConverter 转换器接口
 * <p>
 * 负责字典选项请求、实体和视图对象之间的转换。labels 与 attrsJson 由应用服务处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictItemConverter {

    /**
     * 创建请求转换为字典选项实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "itemLabel", ignore = true)
    @Mapping(target = "attrsJson", ignore = true)
    DictItem toEntity(DictItemCreateRequestDTO request);

    /**
     * 更新请求映射到字典选项实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "dictType", ignore = true)
    @Mapping(target = "itemCode", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "itemLabel", ignore = true)
    @Mapping(target = "attrsJson", ignore = true)
    void updateEntity(DictItemUpdateRequestDTO request, @MappingTarget DictItem entity);

    /**
     * 字典选项实体转换为视图对象
     */
    @Mapping(target = "labels", ignore = true)
    DictItemVO toVO(DictItem entity);

    /**
     * 字典选项实体列表转换为视图对象列表
     */
    List<DictItemVO> toVOList(List<DictItem> entities);
}
