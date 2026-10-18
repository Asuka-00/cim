package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 不存在的 Wafer 不能查历史。
 */
class WaferQueryApplicationServiceTest {

    private WaferRepository waferRepository;
    private WaferQueryApplicationService service;

    @BeforeEach
    void setUp() {
        waferRepository = mock(WaferRepository.class);
        WaferHistoryRepository historyRepository = mock(WaferHistoryRepository.class);
        when(historyRepository.listByWafer(9L)).thenReturn(List.of(new WaferHistory()));
        Wafer wafer = new Wafer();
        wafer.setId(9L);
        when(waferRepository.findById(9L)).thenReturn(Optional.of(wafer));
        service = new WaferQueryApplicationService(waferRepository, historyRepository);
    }

    @Test
    void pageAndHistory() {
        service.page(1, 20, "W", 8L);
        assertThat(service.get(9L).getId()).isEqualTo(9L);
        assertThat(service.history(9L)).hasSize(1);
        verify(waferRepository).page(1, 20, "W", 8L);
    }

    @Test
    void missingWaferRejected() {
        assertThatThrownBy(() -> service.history(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_NOT_FOUND);
    }
}
