package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotConsumptionResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.MaterialConsumption;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.MaterialConsumptionRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 按发生顺序列出消耗，欠账单独标出。Lot 不存在时拒绝。
 */
class LotConsumptionQueryApplicationServiceTest {

    private LotRepository lots;
    private MaterialConsumptionRepository consumptions;
    private LotConsumptionQueryApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        consumptions = mock(MaterialConsumptionRepository.class);
        when(lots.findById(8L)).thenReturn(Optional.of(new Lot()));
        service = new LotConsumptionQueryApplicationService(lots, consumptions);
    }

    @Test
    void listsBackflushAndShortage() {
        when(consumptions.listByLot(8L)).thenReturn(List.of(row(15L, "1.5", "BACKFLUSH", 0), row(16L, "2", "BACKFLUSH", 1)));

        assertThat(service.list(8L)).containsExactly(
                new LotConsumptionResponse(null, 15L, 20L, new BigDecimal("1.5"), "BACKFLUSH", false),
                new LotConsumptionResponse(null, 16L, 20L, new BigDecimal("2"), "BACKFLUSH", true));
    }

    @Test
    void missingLotRejected() {
        when(lots.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(8L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NOT_FOUND);
    }

    private static MaterialConsumption row(long materialId, String qty, String source, int shortage) {
        MaterialConsumption row = new MaterialConsumption();
        row.setMaterialId(materialId);
        row.setMaterialLotId(20L);
        row.setQty(new BigDecimal(qty));
        row.setSource(source);
        row.setIsShortage(shortage);
        return row;
    }
}
