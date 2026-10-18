package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.ProductOutputResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;

/**
 * 只统计这一天完成的 Lot。数量为空按 0。
 */
class DailyOutputApplicationServiceTest {

    private LotRepository lots;
    private DailyOutputApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        service = new DailyOutputApplicationService(lots);
    }

    @Test
    void sumsCompletedLotsOnThatDay() {
        LocalDate day = LocalDate.of(2026, 10, 16);
        when(lots.listCompletedBetween(LocalDateTime.of(2026, 10, 16, 0, 0), LocalDateTime.of(2026, 10, 17, 0, 0)))
                .thenReturn(List.of(completed(2L, 1L), completed(null, null)));

        assertThat(service.summarize(day).lotCount()).isEqualTo(2);
        assertThat(service.summarize(day).qty()).isEqualTo(2);
        assertThat(service.summarize(day).scrapQty()).isEqualTo(1);
    }

    @Test
    void groupsQtyByProduct() {
        LocalDate day = LocalDate.of(2026, 10, 16);
        when(lots.listCompletedBetween(LocalDateTime.of(2026, 10, 16, 0, 0), LocalDateTime.of(2026, 10, 17, 0, 0)))
                .thenReturn(List.of(product(1L, 2L), product(1L, 3L), product(2L, 4L)));

        assertThat(service.byProduct(day)).containsExactly(
                new ProductOutputResponse(1L, 2, 5, 0),
                new ProductOutputResponse(2L, 1, 4, 0));
    }

    private static Lot product(long productId, long qty) {
        Lot lot = completed(qty, 0L);
        lot.setProductId(productId);
        return lot;
    }

    private static Lot completed(Long qty, Long scrapQty) {
        Lot lot = new Lot();
        lot.setStatus("COMPLETED");
        lot.setQty(qty);
        lot.setScrapQty(scrapQty);
        return lot;
    }
}
