package com.scaffold.mes.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Warehouse 库房
 * <p>
 * 对应 mes_warehouse。类型是 RAW、LINESIDE、QUARANTINE 或 OTHER。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_warehouse")
public class Warehouse extends NonSoftDeletableEntity {

    /** 库房编码。 */
    private String whCode;

    /** 库房名称。 */
    private String whName;

    /** RAW、LINESIDE、QUARANTINE 或 OTHER。 */
    private String whType;

    /** 1 表示启用。 */
    private Integer enabled;
}
