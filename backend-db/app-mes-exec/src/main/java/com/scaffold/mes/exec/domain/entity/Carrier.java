package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Carrier 过程载具
 * <p>
 * 对应 mes_carrier。current_lot_id 是 Lot 绑定关系的事实来源。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_carrier")
public class Carrier extends NonSoftDeletableEntity {

    /** 载具编码。 */
    private String carrierCode;

    /** 类型。FOUP、CASSETTE、MAGAZINE、OTHER。 */
    private String carrierType;

    /** 容量。 */
    private Integer capacity;

    /** EMPTY 或 OCCUPIED。 */
    private String status;

    /** 当前绑定的 Lot。 */
    private Long currentLotId;

    /** 累计使用次数。 */
    private Integer useCount;

    /** 1 启用，0 停用。 */
    private Integer enabled;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
