package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 审批记录只追加。动作、业务类型和操作人不合法时拒绝。
 */
class ApprovalRecordApplicationServiceTest {

    private ApprovalRecordRepository repository;
    private ApprovalRecordApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ApprovalRecordRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new ApprovalRecordApplicationService(repository, clock);
    }

    @Test
    void listMineSkipsDecidedSubmits() {
        ApprovalRecord open = submit(1L, 9L);
        ApprovalRecord decided = submit(2L, 8L);
        ApprovalRecord approve = new ApprovalRecord();
        approve.setId(3L);
        when(repository.listSubmits("sam")).thenReturn(List.of(decided, open));
        when(repository.findLatest("ROUTE_VERSION", 8L, "APPROVE")).thenReturn(Optional.of(approve));
        when(repository.findLatest("ROUTE_VERSION", 9L, "APPROVE")).thenReturn(Optional.empty());
        when(repository.findLatest(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.findLatest("ROUTE_VERSION", 8L, "APPROVE")).thenReturn(Optional.of(approve));

        assertThat(service.listMine("sam")).extracting(ApprovalRecord::getBizId).containsExactly(9L);
    }

    @Test
    void recordAppendsSubmit() {
        service.record("ROUTE_VERSION", 9L, "SUBMIT", "DRAFT", "PENDING", "sam", "lee", "请审");

        ArgumentCaptor<ApprovalRecord> captor = ArgumentCaptor.forClass(ApprovalRecord.class);
        verify(repository).save(captor.capture());
        ApprovalRecord saved = captor.getValue();
        assertThat(saved.getBizType()).isEqualTo("ROUTE_VERSION");
        assertThat(saved.getAction()).isEqualTo("SUBMIT");
        assertThat(saved.getOperator()).isEqualTo("lee");
        assertThat(saved.getActionAt()).isEqualTo(java.time.LocalDateTime.of(2026, 10, 16, 12, 0));
    }

    @Test
    void recordRejectsUnknownAction() {
        assertThatThrownBy(() -> service.record("BOM_VERSION", 1L, "CANCEL", null, null, null, "lee", null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.APPROVAL_ACTION_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void recordRejectsBlankOperator() {
        assertThatThrownBy(() -> service.record("RECIPE_VERSION", 1L, "APPROVE", "PENDING", "ACTIVE", null, " ", null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.APPROVAL_OPERATOR_REQUIRED);
    }

    @Test
    void pageRejectsUnknownBizType() {
        assertThatThrownBy(() -> service.page("LOT", 1L, 1, 20))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.APPROVAL_BIZ_TYPE_INVALID);
    }

    private static ApprovalRecord submit(Long id, Long bizId) {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(id);
        record.setBizType("ROUTE_VERSION");
        record.setBizId(bizId);
        record.setAction("SUBMIT");
        record.setAssignee("sam");
        return record;
    }
}
