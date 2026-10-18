package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Wafer 片
 * <p>
 * 对应 mes_wafer。wafer_code 是本厂内部唯一编号。客户编号放 vendor_wafer_id，允许重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_wafer")
public class Wafer extends NonSoftDeletableEntity {

    /** 本厂内部编号。 */
    private String waferCode;

    /** 所属 Lot。 */
    private Long lotId;

    /** Slot 号。 */
    private Integer slotNo;

    /** 状态。 */
    private String status;

    /** 用途。 */
    private String purpose;

    /** 来料客户。 */
    private Long customerId;

    /** 客户 Wafer 编号，不要求唯一。 */
    private String vendorWaferId;

    /** 回收再入厂时指向原片。 */
    private Long recycledFromWaferId;

    /** Dummy 已使用次数。 */
    private Integer dummyUseCount;

    /** 报废原因码。 */
    private String scrapReason;

    /** 报废发生站点。 */
    private Long scrappedStepId;

    /** 报废时间。 */
    private LocalDateTime scrappedAt;

    /** 备注。 */
    private String remark;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
