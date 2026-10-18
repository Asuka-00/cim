package com.scaffold.mes.exec.domain.dto;

/**
 * LotChildResponse 从父 Lot 拆出的子批
 *
 * @param lotId 子 Lot
 * @param lotNo 子批号
 * @param qty 拆出数量
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record LotChildResponse(Long lotId, String lotNo, Long qty) {
}
