package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotPriorityRequest 修改优先级
 * <p>
 * 1 最高，9 最低。Hold 中的在制 Lot 也可以改。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotPriorityRequest {

    /** 优先级，1 到 9。 */
    @NotNull
    private Integer priority;
}
