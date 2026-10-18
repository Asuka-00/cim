package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EcnApproveRequest;
import com.scaffold.mes.base.domain.dto.EcnCreateRequest;
import com.scaffold.mes.base.domain.entity.Ecn;
import com.scaffold.mes.base.domain.repository.EcnRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建变更申请时状态是草稿。类型不合法或编号重复时拒绝。
 */
class EcnApplicationServiceTest {

    private EcnRepository repository;
    private EcnApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EcnRepository.class);
        when(repository.findByNo("ECN-1")).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new EcnApplicationService(repository, clock);
    }

    @Test
    void createsDraft() {
        service.create(request("ROUTE"));

        verify(repository).save(org.mockito.ArgumentMatchers.<Ecn>argThat(row ->
                "DRAFT".equals(row.getStatus()) && "ECN-1".equals(row.getEcnNo()) && "改配方".equals(row.getTitle())));
    }

    @Test
    void badTypeRejected() {
        assertThatThrownBy(() -> service.create(request("TOOL")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_INVALID);
    }

    @Test
    void duplicateNoRejected() {
        when(repository.findByNo("ECN-1")).thenReturn(Optional.of(new Ecn()));

        assertThatThrownBy(() -> service.create(request("BOM")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_INVALID);
    }

    @Test
    void submitMovesDraftToReviewing() {
        Ecn ecn = new Ecn();
        ecn.setStatus("DRAFT");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        service.submit(5L);

        assertThat(ecn.getStatus()).isEqualTo("REVIEWING");
        verify(repository).update(ecn);
    }

    @Test
    void submitRejectsNonDraft() {
        Ecn ecn = new Ecn();
        ecn.setStatus("REVIEWING");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        assertThatThrownBy(() -> service.submit(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_STATUS);
    }

    @Test
    void approveMovesReviewingToApproved() {
        Ecn ecn = new Ecn();
        ecn.setStatus("REVIEWING");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));
        EcnApproveRequest request = new EcnApproveRequest();
        request.setApprover("sam");

        service.approve(5L, request);

        assertThat(ecn.getStatus()).isEqualTo("APPROVED");
        assertThat(ecn.getApprover()).isEqualTo("sam");
    }

    @Test
    void approveRejectsDraft() {
        Ecn ecn = new Ecn();
        ecn.setStatus("DRAFT");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));
        EcnApproveRequest request = new EcnApproveRequest();
        request.setApprover("sam");

        assertThatThrownBy(() -> service.approve(5L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_STATUS);
    }

    @Test
    void rejectMovesReviewingToRejected() {
        Ecn ecn = new Ecn();
        ecn.setStatus("REVIEWING");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));
        EcnApproveRequest request = new EcnApproveRequest();
        request.setApprover("sam");

        service.reject(5L, request);

        assertThat(ecn.getStatus()).isEqualTo("REJECTED");
        assertThat(ecn.getApprover()).isEqualTo("sam");
    }

    @Test
    void rejectRejectsDraft() {
        Ecn ecn = new Ecn();
        ecn.setStatus("DRAFT");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));
        EcnApproveRequest request = new EcnApproveRequest();
        request.setApprover("sam");

        assertThatThrownBy(() -> service.reject(5L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_STATUS);
    }

    @Test
    void effectMovesApprovedToEffective() {
        Ecn ecn = new Ecn();
        ecn.setStatus("APPROVED");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        service.effect(5L);

        assertThat(ecn.getStatus()).isEqualTo("EFFECTIVE");
        assertThat(ecn.getEffectiveAt()).isEqualTo(LocalDateTime.of(2026, 10, 16, 16, 0));
    }

    @Test
    void effectRejectsReviewing() {
        Ecn ecn = new Ecn();
        ecn.setStatus("REVIEWING");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        assertThatThrownBy(() -> service.effect(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_STATUS);
    }

    @Test
    void cancelMovesDraftToCancelled() {
        Ecn ecn = new Ecn();
        ecn.setStatus("DRAFT");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        service.cancel(5L);

        assertThat(ecn.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelRejectsReviewing() {
        Ecn ecn = new Ecn();
        ecn.setStatus("REVIEWING");
        when(repository.findById(5L)).thenReturn(Optional.of(ecn));

        assertThatThrownBy(() -> service.cancel(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ECN_STATUS);
    }

    private static EcnCreateRequest request(String type) {
        EcnCreateRequest request = new EcnCreateRequest();
        request.setEcnNo("ECN-1");
        request.setTitle("改配方");
        request.setChangeType(type);
        request.setReason("良率下降");
        return request;
    }
}
