package com.scaffold.system.domain.vo.system;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DictTypeVO 视图对象
 * <p>
 * 负责向接口调用方返回字典类型信息。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypeVO extends BaseVO {

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
