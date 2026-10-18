package com.scaffold.system.domain.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * DictItem 字典选项实体
 * <p>
 * 对应 sys_dict_item，用于维护某个字典类型下的可选值。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_dict_item")
public class DictItem extends NonSoftDeletableEntity {

    /**
     * 所属字典类型编码
     */
    private String dictType;

    /**
     * 选项编码
     */
    private String itemCode;

    /**
     * 选项显示名称（默认语言，等同 attrs.labels.zh）
     */
    private String itemLabel;

    /**
     * 扩展属性 JSON，多语言显示名存于 labels，例如 {"labels":{"zh":"草稿","en":"Draft"}}
     */
    private String attrsJson;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 前端样式类名
     */
    private String cssClass;

    /**
     * 选项扩展属性 JSON
     */
    private String extraJson;

    /**
     * 启用状态
     */
    private StateEnum enabled;
}
