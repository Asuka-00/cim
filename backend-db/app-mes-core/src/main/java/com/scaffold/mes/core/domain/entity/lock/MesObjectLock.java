package com.scaffold.mes.core.domain.entity.lock;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesObjectLock 跨请求对象锁
 * <p>
 * 对应 mes_object_lock。只用于组批、盘点这类跨请求占用,不用于单次事务内互斥。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_object_lock")
public class MesObjectLock extends NonSoftDeletableEntity {

    /** 对象类型,如 BATCH、LOCATION、WAREHOUSE。 */
    private String objectType;

    /** 对象主键。 */
    private Long objectId;

    /** 持锁人。 */
    private String lockOwner;

    /** 用途,可空。 */
    private String purpose;

    /** 加锁时间。 */
    private LocalDateTime lockedAt;

    /** 过期时间,到点后视为未占用。 */
    private LocalDateTime expireAt;
}
