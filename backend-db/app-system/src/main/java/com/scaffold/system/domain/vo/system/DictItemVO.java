package com.scaffold.system.domain.vo.system;

import java.util.Map;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DictItemVO 视图对象
 * <p>
 * 负责向接口调用方返回字典选项信息。labels 取自 attrs.labels 的全量多语言显示名。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictItemVO extends BaseVO {

    /**
     * 所属字典类型编码
     */
    private String dictType;

    /**
     * 选项编码
     */
    private String itemCode;

    /**
     * 选项显示名称（默认语言，等同 labels.zh / attrs.labels.zh）
     */
    private String itemLabel;

    /**
     * 多语言显示名（来自 attrs.labels），例如 {"zh":"草稿","en":"Draft"}
     */
    private Map<String, String> labels;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 前端样式类名
     */
    private String cssClass;

    /**
     * 扩展属性 JSON
     */
    private String extraJson;

    /**
     * 启用状态
     */
    private StateEnum enabled;
}
