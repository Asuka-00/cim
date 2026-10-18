package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentPort 设备端口
 * <p>
 * 对应 mes_equipment_port。Port 一般沿用设备状态，Chamber 有自己的 E10。当前状态不从台账保存接口改。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_port")
public class EquipmentPort extends NonSoftDeletableEntity {

    /** 所属设备。 */
    private Long equipmentId;

    /** 端口编码，设备内唯一。 */
    private String portCode;

    /** 名称。 */
    private String portName;

    /** 类型：PORT 或 CHAMBER。 */
    private String kind;

    /** 同时加工 Lot 数上限。 */
    private Integer maxConcurrent;

    /** 当前 E10。 */
    private String e10State;

    /** 细分状态码。 */
    private String statusCode;

    /** 停机原因码。 */
    private String reasonCode;

    /** 状态来源：MANUAL、TRACK、SYSTEM。 */
    private String statusSource;

    /** 当前状态起始时间。 */
    private LocalDateTime statusSince;

    /** 1 启用，0 停用。 */
    private Integer enabled;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
