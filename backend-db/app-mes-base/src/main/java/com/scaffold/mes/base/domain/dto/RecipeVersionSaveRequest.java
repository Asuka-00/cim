package com.scaffold.mes.base.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * RecipeVersionSaveRequest Recipe 版本保存请求
 * <p>
 * 只改草稿的参数说明和备注。状态由复制和生效接口改变。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RecipeVersionSaveRequest {

    /** 参数说明，JSON 文本。 */
    private String paramDesc;

    /** 备注。 */
    private String remark;
}
