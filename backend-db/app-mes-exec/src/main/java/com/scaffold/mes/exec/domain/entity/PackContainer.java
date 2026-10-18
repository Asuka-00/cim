package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * PackContainer 包装容器
 * <p>
 * 对应 mes_pack_container。类型是 TRAY、REEL、TUBE 或 BOX。装满后变为 FULL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_pack_container")
public class PackContainer extends NonSoftDeletableEntity {

    /** 容器号。 */
    private String containerNo;

    /** TRAY、REEL、TUBE 或 BOX。 */
    private String containerType;

    /** 所属 Lot。 */
    private Long lotId;

    /** 当前装载数量。 */
    private Integer qty;

    /** 容量。 */
    private Integer capacity;

    /** OPEN、FULL、SHIPPED 或 SCRAPPED。 */
    private String status;

    /** 装满时间。 */
    private LocalDateTime packedAt;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
