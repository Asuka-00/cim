package com.scaffold.system.domain.dto.rbac;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.share.enums.VisibleEnum;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.enums.auth.PermissionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PermissionCreateRequestDTO数据传输对象
 * <p>
 * 负责承载PermissionCreateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class PermissionCreateRequestDTO {

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称长度不能超过100")
    private String permissionName;

    @Size(max = 200, message = "权限描述长度不能超过200")
    private String permissionDescription;

    private PermissionType type;

    @Size(max = 200, message = "路由路径长度不能超过200")
    private String path;

    @Size(max = 100, message = "路由名称长度不能超过100")
    private String pathName;

    @Size(max = 500, message = "路由元数据长度不能超过500")
    private String meta;

    @Size(max = 200, message = "视图文件路径长度不能超过200")
    private String component;

    @Min(value = 0, message = "排序号最小为0")
    private Integer permissionOrder;

    private VisibleEnum visible;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;

    private StateEnum enabled;
}
