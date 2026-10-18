package com.scaffold.mes.base.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.repository.BomItemRepository;

/**
 * 只返回当前站点上打开反冲的 PER_LOT 行。
 */
class BomConsumePortImplTest {

    @Test
    void keepsPerLotBackflushOnThisStep() {
        BomItemRepository items = mock(BomItemRepository.class);
        when(items.listByVersion(4L)).thenReturn(List.of(line("PER_LOT", 1, 7L), line("PER_WAFER", 1, 7L),
                line("PER_DIE", 1, 7L), line("PER_STRIP", 1, 7L), line("PER_TIME", 1, 7L), line("PER_RUN", 1, 7L),
                line("PER_LOT", 0, 7L)));
        BomConsumePortImpl port = new BomConsumePortImpl(items);

        assertThat(port.perRun(4L, 7L)).hasSize(1);
        assertThat(port.perTime(4L, 7L)).hasSize(1);
        assertThat(port.perStrip(4L, 7L)).hasSize(1);
        assertThat(port.perDie(4L, 7L)).hasSize(1);
        assertThat(port.perWafer(4L, 7L)).hasSize(1);
        assertThat(port.perLot(4L, 7L)).hasSize(1);
        assertThat(port.perLot(4L, 7L).get(0).materialId()).isEqualTo(15L);
    }

    private static BomItem line(String mode, int backflush, Long stepId) {
        BomItem item = new BomItem();
        item.setConsumeMode(mode);
        item.setBackflush(backflush);
        item.setStepId(stepId);
        item.setMaterialId(15L);
        item.setQtyPer(new BigDecimal("1.5"));
        return item;
    }
}
