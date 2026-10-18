package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UnitSerial Unit 序列号
 * <p>
 * 对应 mes_unit_serial。只有产品打开跟踪时才写入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_unit_serial")
public class UnitSerial extends NonSoftDeletableEntity {

    /** 序列号。 */
    private String serialNo;

    /** Lot。 */
    private Long lotId;

    /** Strip，可空。 */
    private Long stripId;

    /** 容器，可空。 */
    private Long containerId;

    /** 位置。 */
    private String position;

    /** NORMAL、SCRAPPED 或 SHIPPED。 */
    private String status;
}
