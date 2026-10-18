package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * InterlockLog 进站阻断日志
 * <p>
 * 对应 hist_mes_interlock_log。只追加。通过时不写。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_interlock_log")
public class InterlockLog extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** Lot 号。 */
    private String lotNo;

    /** 站点。 */
    private Long stepId;

    /** 设备。 */
    private Long equipmentId;

    /** 被校验的动作。 */
    private String action;

    /** BLOCK。 */
    private String result;

    /** 规则和原因。 */
    private String detail;

    /** 操作人。 */
    private String operator;

    /** 校验时间。 */
    private LocalDateTime checkedAt;
}
