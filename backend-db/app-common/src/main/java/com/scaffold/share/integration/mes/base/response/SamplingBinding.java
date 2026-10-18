package com.scaffold.share.integration.mes.base.response;

import java.math.BigDecimal;

/**
 * SamplingBinding 站点上生效的抽检规则
 *
 * @param ruleId 规则
 * @param nValue 每 N 个 Lot 抽 1 个。百分比规则为 0
 * @param sampleType EVERY_N_LOT 或 PERCENT
 * @param percent 抽检百分比。间隔规则为空
 * @param waferCount 抽中时要抽的片数。空表示全部
 * @param slotList 指定槽位，逗号分隔。空表示不指定
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record SamplingBinding(Long ruleId, int nValue, String sampleType, BigDecimal percent, Integer waferCount,
        String slotList) {

    /**
     * 按间隔批数的规则，片数和槽位不限。
     *
     * @param ruleId 规则
     * @param nValue 每 N 个 Lot 抽 1 个
     */
    public SamplingBinding(Long ruleId, int nValue) {
        this(ruleId, nValue, "EVERY_N_LOT", null, null, null);
    }

    /**
     * 不限定片数和槽位的规则。
     *
     * @param ruleId 规则
     * @param nValue 间隔。百分比规则为 0
     * @param sampleType 抽检方式
     * @param percent 百分比
     */
    public SamplingBinding(Long ruleId, int nValue, String sampleType, BigDecimal percent) {
        this(ruleId, nValue, sampleType, percent, null, null);
    }

    /**
     * 不指定槽位的规则。
     *
     * @param ruleId 规则
     * @param nValue 间隔
     * @param sampleType 抽检方式
     * @param percent 百分比
     * @param waferCount 片数
     */
    public SamplingBinding(Long ruleId, int nValue, String sampleType, BigDecimal percent, Integer waferCount) {
        this(ruleId, nValue, sampleType, percent, waferCount, null);
    }
}
