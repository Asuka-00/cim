package com.scaffold.mes.exec.domain.dto;

/**
 * WipStepResponse 某个站点上还在制的量
 *
 * @param stepId 站点。空表示 Lot 上没有站点
 * @param lotCount Lot 数
 * @param qty 数量合计
 * @param holdCount 其中被 Hold 的批数
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record WipStepResponse(Long stepId, long lotCount, long qty, long holdCount) {
}
