package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;

/**
 * 不存在的 Lot 不能查历史和 Wafer。
 */
class LotQueryApplicationServiceTest {

    private LotRepository lotRepository;
    private LotHistoryRepository historyRepository;
    private WaferRepository waferRepository;
    private UserScopePort scopes;
    private LotQueryApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        historyRepository = mock(LotHistoryRepository.class);
        waferRepository = mock(WaferRepository.class);
        scopes = mock(UserScopePort.class);
        when(scopes.allowed(eq("sam"), eq("STEP"), any())).thenReturn(true);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        service = new LotQueryApplicationService(lotRepository, historyRepository, waferRepository, scopes, session);
    }

    @Test
    void historyAndWafersRequireLot() {
        Lot lot = new Lot();
        lot.setId(8L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(historyRepository.listByLot(8L)).thenReturn(List.of(new LotHistory()));
        when(waferRepository.listByLot(8L)).thenReturn(List.of(new Wafer()));

        PageDTO<Lot> page = new PageDTO<>(1, 20);
        page.setRecords(List.of(lot));
        when(lotRepository.page(1, 20, "L", "WAITING")).thenReturn(page);
        service.page(1, 20, "L", "WAITING");
        assertThat(service.get(8L).getId()).isEqualTo(8L);
        assertThat(service.history(8L)).hasSize(1);
        assertThat(service.wafers(8L)).hasSize(1);
        verify(lotRepository).page(1, 20, "L", "WAITING");
    }

    @Test
    void pageHidesStepsOutsideScope() {
        Lot inside = lot(1L, 7L);
        Lot outside = lot(2L, 8L);
        when(scopes.allowed("sam", "STEP", 7L)).thenReturn(true);
        when(scopes.allowed("sam", "STEP", 8L)).thenReturn(false);
        PageDTO<Lot> page = new PageDTO<>(1, 20);
        page.setRecords(List.of(inside, outside));
        when(lotRepository.page(1, 20, null, null)).thenReturn(page);

        assertThat(service.page(1, 20, null, null).getRecords()).extracting(Lot::getId).containsExactly(1L);
    }

    @Test
    void pageHidesAreasOutsideScope() {
        Lot inside = lot(1L, 7L);
        inside.setLocationType("AREA");
        inside.setLocationId(4L);
        Lot outside = lot(2L, 7L);
        outside.setLocationType("AREA");
        outside.setLocationId(5L);
        when(scopes.allowed("sam", "AREA", 4L)).thenReturn(true);
        when(scopes.allowed("sam", "AREA", 5L)).thenReturn(false);
        PageDTO<Lot> page = new PageDTO<>(1, 20);
        page.setRecords(List.of(inside, outside));
        when(lotRepository.page(1, 20, null, null)).thenReturn(page);

        assertThat(service.page(1, 20, null, null).getRecords()).extracting(Lot::getId).containsExactly(1L);
    }

    @Test
    void missingLotRejected() {
        assertThatThrownBy(() -> service.history(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NOT_FOUND);
    }

    private static Lot lot(Long id, Long stepId) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setStepId(stepId);
        return lot;
    }
}
