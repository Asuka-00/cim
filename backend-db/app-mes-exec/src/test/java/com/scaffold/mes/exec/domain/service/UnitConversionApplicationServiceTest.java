package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.UnitConversion;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.UnitConversionRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.UnitConvertProvider;
import com.scaffold.share.integration.mes.base.response.UnitConvertFacts;

/**
 * 转换站点按系数核对数量。偏差内切换单位并写历史，超偏差没有原因则拒绝。
 */
class UnitConversionApplicationServiceTest {

    private UnitConversionRepository repository;
    private Wafer wafer;
    private Lot lot;
    private UnitConversionApplicationService service;

    @BeforeEach
    void setUp() {
        UnitConvertProvider conversions = mock(UnitConvertProvider.class);
        repository = mock(UnitConversionRepository.class);
        WaferRepository wafers = mock(WaferRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        when(conversions.facts(21L, 10, 1L, 3L))
                .thenReturn(new UnitConvertFacts(true, 8L, new BigDecimal("1000"), new BigDecimal("5")));
        wafer = new Wafer();
        wafer.setId(1L);
        wafer.setStatus("NORMAL");
        when(wafers.listByLot(8L)).thenReturn(List.of(wafer));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.ofHours(8));
        lot = new Lot();
        lot.setId(8L);
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setStepId(7L);
        lot.setProductId(1L);
        lot.setQtyUnitId(3L);
        lot.setQty(2L);
        lot.setCurrentScrapQty(1L);
        service = new UnitConversionApplicationService(conversions, repository, wafers, session, clock);
    }

    @Test
    void withinToleranceSwitchesUnit() {
        TrackOutRequest request = new TrackOutRequest();
        request.setQtyAfter(2000L);

        service.apply(lot, request);

        assertThat(lot.getQty()).isEqualTo(2000L);
        assertThat(lot.getQtyUnitId()).isEqualTo(8L);
        assertThat(lot.getCurrentScrapQty()).isZero();
        assertThat(wafer.getStatus()).isEqualTo("CONVERTED");
        verify(repository).save(org.mockito.ArgumentMatchers.<UnitConversion>argThat(row ->
                row.getQtyReference().equals(2000L) && row.getFromUnitId().equals(3L) && row.getLossQty().equals(0L)));
    }

    @Test
    void deviationWithoutReasonRejected() {
        TrackOutRequest request = new TrackOutRequest();
        request.setQtyAfter(1000L);

        assertThatThrownBy(() -> service.apply(lot, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UNIT_CONVERT_DEVIATION);
        assertThat(lot.getQty()).isEqualTo(2L);
        assertThat(wafer.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    void deviationWithReasonConverts() {
        TrackOutRequest request = new TrackOutRequest();
        request.setQtyAfter(1000L);
        request.setConfirmReason("抽测损耗");

        service.apply(lot, request);

        assertThat(lot.getQty()).isEqualTo(1000L);
        assertThat(lot.getQtyUnitId()).isEqualTo(8L);
        assertThat(wafer.getStatus()).isEqualTo("CONVERTED");
        verify(repository).save(org.mockito.ArgumentMatchers.<UnitConversion>argThat(row ->
                "抽测损耗".equals(row.getConfirmReason()) && row.getQtyAfter().equals(1000L)
                        && row.getDeviationPct().compareTo(new BigDecimal("5")) > 0));
    }
}
