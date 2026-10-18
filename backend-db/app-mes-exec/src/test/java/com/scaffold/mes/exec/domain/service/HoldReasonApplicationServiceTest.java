package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.HoldReasonSaveRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Hold 原因码不能重复。类别非法或仍被 Lot Hold 引用时拒绝。
 */
class HoldReasonApplicationServiceTest {

    private HoldReasonRepository repository;
    private HoldReasonApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(HoldReasonRepository.class);
        service = new HoldReasonApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("QC-HOLD")).thenReturn(Optional.of(new HoldReason()));

        assertThatThrownBy(() -> service.create(request("QC-HOLD", "QUALITY")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HOLD_REASON_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownType() {
        assertThatThrownBy(() -> service.create(request("QC-HOLD", "SAFETY")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HOLD_REASON_TYPE_INVALID);
    }

    @Test
    void createDefaultsTypeToOther() {
        service.create(request("MISC", " "));

        verify(repository).save(org.mockito.ArgumentMatchers.<HoldReason>argThat(reason -> "OTHER".equals(reason.getHoldType())
                && reason.getReleaseRoleCode() == null && reason.getEnabled() == 1));
    }

    @Test
    void deleteRejectsWhenHoldUsesCode() {
        HoldReason reason = new HoldReason();
        reason.setId(1L);
        reason.setReasonCode("QC-HOLD");
        when(repository.findById(1L)).thenReturn(Optional.of(reason));
        when(repository.countHolds("QC-HOLD")).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HOLD_REASON_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        HoldReason reason = new HoldReason();
        reason.setId(1L);
        reason.setReasonCode("QC-HOLD");
        when(repository.findById(1L)).thenReturn(Optional.of(reason));
        when(repository.countHolds("QC-HOLD")).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
        assertThat(reason.getReasonCode()).isEqualTo("QC-HOLD");
    }

    private static HoldReasonSaveRequest request(String code, String type) {
        HoldReasonSaveRequest request = new HoldReasonSaveRequest();
        request.setReasonCode(code);
        request.setReasonName("质量 Hold");
        request.setHoldType(type);
        request.setReleaseRoleCode(" ");
        return request;
    }
}
