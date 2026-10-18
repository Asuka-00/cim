package com.scaffold.system.domain.dto.system;

import java.util.Map;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DictItemUpdateRequestDTO 数据传输对象
 * <p>
 * 负责承载更新字典选项的请求字段，选项编码不允许通过更新接口变更。
 * 显示名优先使用 labels（含 zh），兼容单语 itemLabel。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
public class DictItemUpdateRequestDTO {

    /**
     * 选项显示名称（默认语言，兼容旧客户端；优先使用 labels.zh）
     */
    @Size(max = 128, message = "选项显示名称长度不能超过128")
    private String itemLabel;

    /**
     * 多语言显示名，key 为 locale（zh/en），至少应包含 zh
     */
    private Map<String, String> labels;

    /**
     * 排序号
     */
    @Min(value = 0, message = "排序号最小为0")
    private Integer sortOrder;

    /**
     * 前端样式类名
     */
    @Size(max = 64, message = "样式长度不能超过64")
    private String cssClass;

    /**
     * 扩展属性 JSON
     */
    private String extraJson;

    /**
     * 启用状态
     */
    @NotNull(message = "启用状态不能为空")
    private StateEnum enabled;
}
