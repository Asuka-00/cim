package com.scaffold.system.domain.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.SoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SystemConfig 系统配置实体
 * <p>
 * 记录系统级键值对配置，支持分组、值类型和启用状态管理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_system_config")
public class SystemConfig extends SoftDeletableEntity {

    /**
     * 配置编码
     */
    private String configCode;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 值类型（string/int/boolean/json等）
     */
    private String valueType;

    /**
     * 配置分组
     */
    private String groupCode;

    /**
     * 配置描述
     */
    private String remark;

    /**
     * 是否启用（1:启用 0:禁用）
     */
    private StateEnum state;
}
