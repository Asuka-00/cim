package com.scaffold.system.domain.dto.system;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SystemConfigCreateRequestDTO数据传输对象
 * <p>
 * 负责承载SystemConfigCreateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class SystemConfigCreateRequestDTO {

    @NotBlank(message = "配置编码不能为空")
    @Size(max = 60, message = "配置编码长度不能超过60")
    private String configCode;

    /** 允许空串（如 brand.*-svg 清空回退静态默认）；上限覆盖 SVG 文本 */
    @Size(max = 65535, message = "配置值长度不能超过65535")
    private String configValue;

    @NotBlank(message = "值类型不能为空")
    @Size(max = 20, message = "值类型长度不能超过20")
    private String valueType;

    @NotBlank(message = "配置分组不能为空")
    @Size(max = 100, message = "配置分组长度不能超过100")
    private String groupCode;

    @Size(max = 200, message = "配置描述长度不能超过200")
    private String remark;

    @NotNull(message = "状态不能为空")
    private StateEnum state;
}
