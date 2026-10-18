package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.PackContainerCreateRequest;
import com.scaffold.mes.exec.domain.dto.PackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.PackContainer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.PackContainerRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 包装容器装到容量后变为 FULL。超过容量时保持原数量。
 */
class PackContainerApplicationServiceTest {

    private PackContainerRepository containers;
    private PackContainer container;
    private PackContainerApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lots = mock(LotRepository.class);
        containers = mock(PackContainerRepository.class);
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        when(lots.findById(8L)).thenReturn(Optional.of(lot));
        when(containers.findByNo("R-1")).thenReturn(Optional.empty());
        container = new PackContainer();
        container.setId(4L);
        container.setStatus("OPEN");
        container.setQty(90);
        container.setCapacity(100);
        when(containers.findById(4L)).thenReturn(Optional.of(container));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.ofHours(8));
        service = new PackContainerApplicationService(lots, containers, clock);
    }

    @Test
    void packToCapacityMarksFull() {
        PackRequest request = new PackRequest();
        request.setQty(10);

        service.pack(4L, request);

        assertThat(container.getQty()).isEqualTo(100);
        assertThat(container.getStatus()).isEqualTo("FULL");
        assertThat(container.getPackedAt()).isNotNull();
    }

    @Test
    void overCapacityRejected() {
        PackRequest request = new PackRequest();
        request.setQty(11);

        assertThatThrownBy(() -> service.pack(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.PACK_QTY);
        assertThat(container.getStatus()).isEqualTo("OPEN");
        assertThat(container.getQty()).isEqualTo(90);
        verify(containers, never()).update(container);
    }

    @Test
    void createSavesOpenReel() {
        PackContainerCreateRequest request = new PackContainerCreateRequest();
        request.setContainerNo("R-1");
        request.setContainerType("REEL");
        request.setCapacity(100);

        service.create(8L, request);

        verify(containers).save(org.mockito.ArgumentMatchers.<PackContainer>argThat(row ->
                "REEL".equals(row.getContainerType()) && "OPEN".equals(row.getStatus()) && row.getQty().equals(0)
                        && row.getLotId().equals(8L)));
    }
}
