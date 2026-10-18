package com.scaffold.mes.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StorageLocation 库位
 * <p>
 * 对应 mes_location。未启用的库位不能收货。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_location")
public class StorageLocation extends NonSoftDeletableEntity {

    /** 库房。 */
    private Long whId;

    /** 库位编码。 */
    private String locCode;

    /** 库位名称。 */
    private String locName;

    /** 1 表示启用。 */
    private Integer enabled;
}
