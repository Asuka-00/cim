package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.WipStepResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;

/**
 * 同一站点的等待和加工中 Lot 合成一行。
 */
class WipByStepApplicationServiceTest {

    private LotRepository lots;
    private WipByStepApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        service = new WipByStepApplicationService(lots);
    }

    @Test
    void groupsInProcessLotsByStep() {
        Lot held = lot(7L, 2L);
        held.setHoldCount(1);
        when(lots.listInProcess()).thenReturn(List.of(held, lot(7L, 3L), lot(9L, null)));

        assertThat(service.summarize()).containsExactly(
                new WipStepResponse(7L, 2, 5, 1),
                new WipStepResponse(9L, 1, 0, 0));
    }

    private static Lot lot(long stepId, Long qty) {
        Lot lot = new Lot();
        lot.setStepId(stepId);
        lot.setQty(qty);
        return lot;
    }
}
