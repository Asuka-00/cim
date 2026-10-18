package com.scaffold.system.domain.query.system;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * DictTypeQuery 查询对象
 * <p>
 * 负责承载字典类型分页查询过滤条件。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
public class DictTypeQuery {

    /**
     * 字典类型编码
     */
    private String dictType;

    /**
     * 字典类型名称
     */
    private String dictName;

    /**
     * 启用状态
     */
    private StateEnum enabled;
}
