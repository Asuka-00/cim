package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotNoteRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotNote;
import com.scaffold.mes.exec.domain.repository.LotNoteRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Hold 中的 Lot 可以追加备注。不认识的类型会被拒绝。
 */
class LotNoteApplicationServiceTest {

    private LotNoteRepository noteRepository;
    private LotNoteApplicationService service;

    @BeforeEach
    void setUp() {
        noteRepository = mock(LotNoteRepository.class);
        LotRepository lotRepository = mock(LotRepository.class);
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setHoldCount(2);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        service = new LotNoteApplicationService(noteRepository, lotRepository);
    }

    @Test
    void addsPinnedWarning() {
        LotNoteRequest request = new LotNoteRequest();
        request.setNoteType("WARNING");
        request.setContent("check edge");
        request.setPinned(1);

        service.add(8L, request);

        verify(noteRepository).save(org.mockito.ArgumentMatchers.<LotNote>argThat(row ->
                "WARNING".equals(row.getNoteType()) && row.getPinned() == 1 && row.getLotId().equals(8L)));
    }

    @Test
    void unknownTypeRejected() {
        LotNoteRequest request = new LotNoteRequest();
        request.setNoteType("SECRET");
        request.setContent("x");

        assertThatThrownBy(() -> service.add(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NOTE_TYPE_INVALID);
    }
}
