package com.scaffold.mes.exec.domain.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * LotSplitRequest 拆批
 * <p>
 * 选出要拆走的片。父 Lot 至少留下一片。子 Lot 号由父 Lot 号和拆批序号生成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotSplitRequest {

    /** 要拆走的 Wafer。 */
    @NotEmpty
    private List<Long> waferIds;
}
