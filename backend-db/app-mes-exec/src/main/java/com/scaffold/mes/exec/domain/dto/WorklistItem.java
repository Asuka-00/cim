package com.scaffold.mes.exec.domain.dto;

import java.time.LocalDateTime;

/**
 * WorklistItem 作业清单中的一批
 * <p>
 * 只包含待加工队列要看的字段。
 *
 * @param lotId Lot
 * @param lotNo Lot 号
 * @param priority 优先级，1 最高
 * @param isHot 1 表示 Hot Lot
 * @param stepId 当前站点
 * @param stepEnterAt 进入当前站点的时间
 * @param qty 当前数量
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record WorklistItem(
        Long lotId,
        String lotNo,
        Integer priority,
        Integer isHot,
        Long stepId,
        LocalDateTime stepEnterAt,
        Long qty) {
}
