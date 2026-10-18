package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.DispatchRule;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.DispatchRuleRepository;

/**
 * 默认规则把 Hot Lot 排在前面。不认识的排序键会被跳过。
 */
class DispatchRuleApplicationServiceTest {

    @Test
    void defaultRulePutsHotLotFirst() {
        DispatchRuleRepository repository = mock(DispatchRuleRepository.class);
        DispatchRule rule = new DispatchRule();
        rule.setEnabled(1);
        rule.setSortKeys("IS_HOT:DESC,PRIORITY:ASC,QUEUE_URGENCY:DESC");
        when(repository.findDefault()).thenReturn(Optional.of(rule));
        DispatchRuleApplicationService service = new DispatchRuleApplicationService(repository);
        Lot hot = lot(1L, 0, 9);
        Lot cold = lot(2L, 0, 1);
        Lot laterHot = lot(3L, 1, 5);

        List<Long> order = List.of(laterHot, cold, hot).stream().sorted(service.comparator(null)).map(Lot::getId).toList();

        assertThat(order).containsExactly(3L, 2L, 1L);
    }

    private static Lot lot(Long id, int hot, int priority) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setIsHot(hot);
        lot.setPriority(priority);
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, 8, 0));
        return lot;
    }
}
