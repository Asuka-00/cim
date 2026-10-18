package com.scaffold.mes.base.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteVersionSaveRequest Route 版本草稿保存请求
 * <p>
 * 只改备注。步骤在版本生效前另行维护。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteVersionSaveRequest {

    /** 备注。 */
    private String remark;
}
