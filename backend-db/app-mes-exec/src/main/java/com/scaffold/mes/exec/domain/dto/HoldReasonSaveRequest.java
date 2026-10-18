package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * HoldReasonSaveRequest Hold 原因保存请求
 * <p>
 * 新增与修改共用。未传类别时按 OTHER，未传启用状态时按启用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class HoldReasonSaveRequest {

    /** 原因码，租户内唯一。 */
    @NotBlank
    private String reasonCode;

    /** 原因名称。 */
    @NotBlank
    private String reasonName;

    /** 类别。空则按 OTHER。 */
    private String holdType;

    /** 允许 Release 的角色。空表示不限。 */
    private String releaseRoleCode;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用。 */
    private Integer enabled;
}
