package com.scaffold.system.domain.vo.rbac;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.share.enums.VisibleEnum;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.enums.auth.PermissionType;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PermissionVO视图对象
 * <p>
 * 负责承载PermissionVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PermissionVO extends BaseVO {

    private String permissionCode;

    private String permissionName;

    private String permissionDescription;

    private PermissionType type;

    private String path;

    private String pathName;

    private String meta;

    private String component;

    private Integer permissionOrder;

    private VisibleEnum visible;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;

    private StateEnum enabled;
}
