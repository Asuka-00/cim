package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Carrier;
import com.scaffold.mes.exec.domain.entity.CarrierBinding;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.CarrierBindingRepository;
import com.scaffold.mes.exec.domain.repository.CarrierRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 一个载具同时只绑一个 Lot。解绑只回填绑定历史的结束时间。
 */
class CarrierApplicationServiceTest {

    private CarrierRepository carrierRepository;
    private CarrierBindingRepository bindingRepository;
    private Carrier carrier;
    private Lot lot;
    private CarrierBinding open;
    private CarrierApplicationService service;

    @BeforeEach
    void setUp() {
        carrierRepository = mock(CarrierRepository.class);
        bindingRepository = mock(CarrierBindingRepository.class);
        LotRepository lotRepository = mock(LotRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        carrier = new Carrier();
        carrier.setId(3L);
        carrier.setCarrierCode("C-1");
        carrier.setStatus("EMPTY");
        carrier.setEnabled(1);
        carrier.setUseCount(0);
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        open = new CarrierBinding();
        open.setCarrierId(3L);
        open.setLotId(8L);
        when(carrierRepository.findById(3L)).thenReturn(Optional.of(carrier));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(bindingRepository.findOpen(3L)).thenReturn(Optional.of(open));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T19:00:00Z"), ZoneOffset.ofHours(8));
        service = new CarrierApplicationService(carrierRepository, bindingRepository, lotRepository, session, clock);
    }

    @Test
    void bindSetsCarrierAsSourceOfTruth() {
        service.bind(3L, 8L);

        assertThat(carrier.getCurrentLotId()).isEqualTo(8L);
        assertThat(carrier.getStatus()).isEqualTo("OCCUPIED");
        assertThat(carrier.getUseCount()).isEqualTo(1);
        assertThat(lot.getCarrierCode()).isEqualTo("C-1");
        verify(bindingRepository).save(org.mockito.ArgumentMatchers.<CarrierBinding>argThat(row -> row.getUnboundAt() == null));
    }

    @Test
    void secondBindRejectedAndUnbindClosesSegment() {
        carrier.setStatus("OCCUPIED");
        carrier.setCurrentLotId(8L);
        assertThatThrownBy(() -> service.bind(3L, 8L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CARRIER_OCCUPIED);

        service.unbind(3L);

        assertThat(carrier.getCurrentLotId()).isNull();
        assertThat(carrier.getStatus()).isEqualTo("EMPTY");
        assertThat(lot.getCarrierCode()).isNull();
        assertThat(open.getUnboundAt()).isNotNull();
        assertThat(open.getUnboundBy()).isEqualTo("sam");
    }
}
