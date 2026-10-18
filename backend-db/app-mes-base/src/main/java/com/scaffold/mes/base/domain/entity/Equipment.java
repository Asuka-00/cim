package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Equipment 设备台账
 * <p>
 * 对应 mes_equipment。当前 E10 状态是冗余字段，变更必须走状态接口并写入历史时间段。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment")
public class Equipment extends NonSoftDeletableEntity {

    /** 设备编码，租户内唯一。 */
    private String equipmentCode;

    /** 设备名称。 */
    private String equipmentName;

    /** 类型：PROCESS、MEASURE、TEST、PACK、TRANSPORT、OTHER。 */
    private String equipmentType;

    /** 设备组，可空。 */
    private Long groupId;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 设备同时加工 Lot 数上限。 */
    private Integer maxConcurrentLots;

    /** 厂商。 */
    private String vendor;

    /** 型号。 */
    private String model;

    /** 序列号。 */
    private String serialNo;

    /** 当前 E10 大类。 */
    private String e10State;

    /** 当前细分状态码。 */
    private String statusCode;

    /** 当前原因码。停机类状态必填。 */
    private String reasonCode;

    /** 当前状态来源：MANUAL、TRACK、SYSTEM。 */
    private String statusSource;

    /** 当前状态起始时间。 */
    private LocalDateTime statusSince;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
