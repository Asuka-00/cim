package com.scaffold.system.domain.dto.system;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DictTypeUpdateRequestDTO 数据传输对象
 * <p>
 * 负责承载更新字典类型的请求字段，字典类型编码不允许通过更新接口变更。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
public class DictTypeUpdateRequestDTO {

    /**
     * 字典类型名称
     */
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 128, message = "字典名称长度不能超过128")
    private String dictName;

    /**
     * 备注
     */
    @Size(max = 256, message = "备注长度不能超过256")
    private String remark;

    /**
     * 启用状态
     */
    @NotNull(message = "启用状态不能为空")
    private StateEnum enabled;
}
