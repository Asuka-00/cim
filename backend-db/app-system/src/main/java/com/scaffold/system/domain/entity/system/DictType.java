package com.scaffold.system.domain.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * DictType 字典类型实体
 * <p>
 * 对应 sys_dict_type，用于维护字典分类和启用状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_dict_type")
public class DictType extends NonSoftDeletableEntity {

    /**
     * 字典类型编码
     */
    private String dictType;

    /**
     * 字典类型名称
     */
    private String dictName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 启用状态
     */
    private StateEnum enabled;
}
