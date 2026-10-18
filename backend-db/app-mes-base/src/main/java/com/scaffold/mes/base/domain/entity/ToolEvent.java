package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ToolEvent 工具事件
 * <p>
 * 对应 hist_mes_tool_event。只追加。出站使用记为 USE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_tool_event")
public class ToolEvent extends AppendOnlyEntity {

    /** 工具。 */
    private Long toolId;

    /** USE 等。 */
    private String eventType;

    /** 当时装载的设备。 */
    private Long equipmentId;

    /** 本次使用量。 */
    private Long usageQty;

    /** 事件后的累计使用量。 */
    private Long usedTotalAfter;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
