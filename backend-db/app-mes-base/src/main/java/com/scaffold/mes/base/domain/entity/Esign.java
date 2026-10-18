package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Esign 电子签名
 * <p>
 * 对应 hist_mes_esign。只追加。需要确认的动作才写。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_esign")
public class Esign extends AppendOnlyEntity {

    /** 动作编码。 */
    private String actionCode;

    /** 业务对象类型。 */
    private String bizType;

    /** 业务对象。 */
    private Long bizId;

    /** 签名人。 */
    private String signer;

    /** 签名人角色。 */
    private String signerRole;

    /** PRIMARY 或 SECOND。 */
    private String signType;

    /** 签名时间。 */
    private LocalDateTime signedAt;
}
