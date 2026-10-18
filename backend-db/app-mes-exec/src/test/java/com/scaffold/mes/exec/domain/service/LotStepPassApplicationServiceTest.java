package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 经过次数按站点显式加减。到达时的序号是已完成次数加 1。
 */
class LotStepPassApplicationServiceTest {

    private LotStepPassRepository repository;
    private LotStepPassApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LotStepPassRepository.class);
        service = new LotStepPassApplicationService(repository);
    }

    @Test
    void arrivalWithoutRowIsFirstPass() {
        when(repository.find(1L, 7L)).thenReturn(Optional.empty());

        assertThat(service.arrivalPassNo(1L, 7L)).isEqualTo(1);
    }

    @Test
    void firstTrackOutInsertsOne() {
        when(repository.find(1L, 7L)).thenReturn(Optional.empty());

        service.recordTrackOut(1L, 7L);

        verify(repository).save(org.mockito.ArgumentMatchers.<LotStepPass>argThat(row -> row.getPassCount().equals(1)
                && row.getLotId().equals(1L)
                && row.getStepId().equals(7L)));
    }

    @Test
    void laterTrackOutIncrements() {
        LotStepPass row = row(2);
        when(repository.find(1L, 7L)).thenReturn(Optional.of(row));

        service.recordTrackOut(1L, 7L);

        assertThat(row.getPassCount()).isEqualTo(3);
        verify(repository).update(row);
        assertThat(service.arrivalPassNo(1L, 7L)).isEqualTo(4);
    }

    @Test
    void undoDecrementsAndEmptyRejects() {
        LotStepPass row = row(1);
        when(repository.find(1L, 7L)).thenReturn(Optional.of(row));
        service.undoTrackOut(1L, 7L);
        assertThat(row.getPassCount()).isZero();

        row.setPassCount(0);
        assertThatThrownBy(() -> service.undoTrackOut(1L, 7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_PASS_NOTHING_TO_UNDO);
    }

    private static LotStepPass row(int count) {
        LotStepPass row = new LotStepPass();
        row.setLotId(1L);
        row.setStepId(7L);
        row.setPassCount(count);
        return row;
    }
}
