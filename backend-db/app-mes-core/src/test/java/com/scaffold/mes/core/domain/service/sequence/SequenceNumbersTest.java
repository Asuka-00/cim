package com.scaffold.mes.core.domain.service.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.share.exception.BusinessException;

/**
 * SequenceNumbers 的格式与进位规则。
 */
class SequenceNumbersTest {

    @Test
    void formatsLotNumberAndPeriod() {
        assertThat(SequenceNumbers.periodKey("MONTH", LocalDate.of(2026, 10, 16))).isEqualTo("202610");
        assertThat(SequenceNumbers.periodKey("NEVER", LocalDate.of(2026, 10, 16))).isEqualTo("-");
        assertThat(SequenceNumbers.nextValue(0, 1, 1)).isEqualTo(1);
        assertThat(SequenceNumbers.nextValue(3, 1, 1)).isEqualTo(4);

        SequenceRule rule = new SequenceRule();
        rule.setRuleCode("LOT");
        rule.setPrefix("L");
        rule.setDatePattern("yyMM");
        rule.setSeqLength(5);
        rule.setSuffix("");
        assertThat(SequenceNumbers.format(rule, 12, LocalDate.of(2026, 10, 16))).isEqualTo("L261000012");
    }

    @Test
    void rejectsSequenceWiderThanLength() {
        SequenceRule rule = new SequenceRule();
        rule.setRuleCode("WORK_ORDER");
        rule.setPrefix("WO");
        rule.setSeqLength(2);
        assertThatThrownBy(() -> SequenceNumbers.format(rule, 100, LocalDate.of(2026, 10, 16)))
                .isInstanceOf(BusinessException.class);
    }
}
