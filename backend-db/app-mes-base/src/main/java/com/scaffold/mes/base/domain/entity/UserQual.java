package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UserQual 人员资质
 * <p>
 * 对应 mes_user_qual。没有记录表示不限制。到期或吊销后视为无效。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_user_qual")
public class UserQual extends NonSoftDeletableEntity {

    /** 用户名。 */
    private String userName;

    /** EQUIPMENT、EQUIPMENT_GROUP 或 STEP。 */
    private String targetType;

    /** 目标对象。 */
    private Long targetId;

    /** VALID、EXPIRED 或 REVOKED。 */
    private String status;

    /** 生效止。空表示不限期。 */
    private LocalDateTime validTo;
}
