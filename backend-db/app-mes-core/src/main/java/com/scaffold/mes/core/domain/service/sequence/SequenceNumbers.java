package com.scaffold.mes.core.domain.service.sequence;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.share.exception.BusinessException;

/**
 * SequenceNumbers 编号格式化
 * <p>
 * 只负责周期键、流水值和单号拼装,不访问数据库。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class SequenceNumbers {

    /** Lot 号长度上限,为多级拆批后缀预留位置。 */
    public static final int LOT_NO_MAX = 48;

    private SequenceNumbers() {
    }

    /**
     * 按重置周期生成计数行的周期键。
     *
     * @param resetCycle NEVER、DAY、MONTH 或 YEAR
     * @param date 取号日期
     * @return 周期键
     */
    public static String periodKey(String resetCycle, LocalDate date) {
        return switch (resetCycle == null ? "NEVER" : resetCycle) {
            case "DAY" -> date.format(DateTimeFormatter.BASIC_ISO_DATE);
            case "MONTH" -> date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "YEAR" -> Integer.toString(date.getYear());
            default -> "-";
        };
    }

    /**
     * 计算下一次发放的流水值。当前值小于起始值时从起始值开始。
     *
     * @param currentValue 已发放的最大流水
     * @param seqStart 起始值
     * @param seqStep 步长
     * @return 下一个流水值
     */
    public static long nextValue(long currentValue, long seqStart, int seqStep) {
        if (currentValue < seqStart) {
            return seqStart;
        }
        return currentValue + seqStep;
    }

    /**
     * 把流水格式化成业务单号。
     *
     * @param rule 编号规则
     * @param value 流水值
     * @param date 取号日期
     * @return 业务单号
     * @throws BusinessException 流水超出位数,或 Lot 号超过长度上限
     */
    public static String format(SequenceRule rule, long value, LocalDate date) {
        String datePart = "";
        if (rule.getDatePattern() != null && !rule.getDatePattern().isBlank()) {
            datePart = date.format(DateTimeFormatter.ofPattern(rule.getDatePattern()));
        }
        String seq = Long.toString(value);
        int length = rule.getSeqLength() == null ? 4 : rule.getSeqLength();
        if (seq.length() > length) {
            throw new BusinessException(MesErrorCodes.SEQUENCE_OVERFLOW, "流水号超出位数: " + rule.getRuleCode());
        }
        seq = "0".repeat(length - seq.length()) + seq;
        String prefix = rule.getPrefix() == null ? "" : rule.getPrefix();
        String suffix = rule.getSuffix() == null ? "" : rule.getSuffix();
        String number = prefix + datePart + seq + suffix;
        if ("LOT".equals(rule.getRuleCode()) && number.length() > LOT_NO_MAX) {
            throw new BusinessException(MesErrorCodes.SEQUENCE_OVERFLOW, "Lot 号超过 " + LOT_NO_MAX + " 位");
        }
        return number;
    }
}
