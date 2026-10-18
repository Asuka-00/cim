package com.scaffold.system.domain.query.system;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * DictItemQuery 查询对象
 * <p>
 * 负责承载字典选项分页查询过滤条件。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
public class DictItemQuery {

    /**
     * 字典类型编码
     */
    private String dictType;

    /**
     * 选项编码
     */
    private String itemCode;

    /**
     * 选项显示名称
     */
    private String itemLabel;

    /**
     * 启用状态
     */
    private StateEnum enabled;
}
