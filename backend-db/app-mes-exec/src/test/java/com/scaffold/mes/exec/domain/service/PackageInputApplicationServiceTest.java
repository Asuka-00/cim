package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.PackageInputRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 封装投入扣减源 Lot。扣完变为 CONSUMED，部分投入保留余量。
 */
class PackageInputApplicationServiceTest {

    private LotRepository lotRepository;
    private LotRelationRepository relations;
    private Lot source;
    private Lot target;
    private PackageInputApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        relations = mock(LotRelationRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        source = lot(9L, 10L);
        target = lot(8L, 1L);
        target.setWoId(3L);
        when(lotRepository.findById(9L)).thenReturn(Optional.of(source));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(target));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"), ZoneOffset.ofHours(8));
        service = new PackageInputApplicationService(transactions, lotRepository, relations,
                mock(LotHistoryApplicationService.class), clock);
    }

    @Test
    void partialInputKeepsRemainder() {
        service.input(8L, request(4L));

        assertThat(source.getQty()).isEqualTo(6L);
        assertThat(source.getStatus()).isEqualTo("WAITING");
        assertThat(target.getQty()).isEqualTo(5L);
        verify(lotRepository).update(source);
    }

    @Test
    void fullInputConsumesSource() {
        service.input(8L, request(10L));

        assertThat(source.getQty()).isZero();
        assertThat(source.getStatus()).isEqualTo("CONSUMED");
        verify(relations).save(org.mockito.ArgumentMatchers.<LotRelation>argThat(row ->
                "PACKAGE_INPUT".equals(row.getRelationType()) && row.getQty().equals(10L)
                        && row.getParentLotId().equals(9L) && row.getChildLotId().equals(8L)));
    }

    @Test
    void tooMuchRejected() {
        assertThatThrownBy(() -> service.input(8L, request(11L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.PACKAGE_INPUT_QTY);
        assertThat(source.getQty()).isEqualTo(10L);
        verify(lotRepository, never()).update(any());
    }

    private static PackageInputRequest request(long qty) {
        PackageInputRequest request = new PackageInputRequest();
        request.setSourceLotId(9L);
        request.setQty(qty);
        return request;
    }

    private static Lot lot(Long id, long qty) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setStatus("WAITING");
        lot.setHoldCount(0);
        lot.setQty(qty);
        return lot;
    }
}
