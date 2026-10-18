package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Tool 工具
 * <p>
 * 对应 mes_tool。领用后装到设备上，状态变为 IN_USE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_tool")
public class Tool extends NonSoftDeletableEntity {

    /** 工具编码。 */
    private String toolCode;

    /** 工具名称。 */
    private String toolName;

    /** 工具类型。 */
    private Long typeId;

    /** AVAILABLE、IN_USE、CLEANING、MAINTENANCE 或 SCRAPPED。 */
    private String status;

    /** 当前装载设备。 */
    private Long currentEquipmentId;

    /** 累计使用量。 */
    private Long usedCount;

    /** 寿命上限。空表示不限。 */
    private Long lifeLimit;

    /** 1 表示启用。 */
    private Integer enabled;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
