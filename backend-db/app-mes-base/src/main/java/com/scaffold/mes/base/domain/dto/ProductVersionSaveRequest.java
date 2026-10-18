package com.scaffold.mes.base.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * ProductVersionSaveRequest 产品版本草稿保存请求
 * <p>
 * 绑定 Route 和 BOM 主数据。生效前两者都必须已有 ACTIVE 版本。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ProductVersionSaveRequest {

    /** Route 主数据。可先留空，提交前要补上。 */
    private Long routeId;

    /** BOM 主数据。可先留空，提交前要补上。 */
    private Long bomId;

    /** 备注。 */
    private String remark;
}
