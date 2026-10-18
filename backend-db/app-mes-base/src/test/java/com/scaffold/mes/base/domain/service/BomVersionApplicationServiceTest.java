package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * BOM 草稿提交后由指定审批人生效。旧 ACTIVE 同时失效。
 */
class BomVersionApplicationServiceTest {

    private final List<BomVersion> rows = new ArrayList<>();
    private BomVersionRepository repository;
    private BomItemRepository itemRepository;
    private ApprovalRecordApplicationService approvalService;
    private BomVersionApplicationService service;

    @BeforeEach
    void setUp() {
        rows.add(version(10L, "ACTIVE"));
        rows.add(version(11L, "DRAFT"));
        repository = mock(BomVersionRepository.class);
        itemRepository = mock(BomItemRepository.class);
        approvalService = mock(ApprovalRecordApplicationService.class);
        ApprovalRecordRepository approvalRepository = mock(ApprovalRecordRepository.class);
        when(repository.findById(any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(repository.listActive(1L)).thenAnswer(invocation -> rows.stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .toList());
        ApprovalRecord submit = new ApprovalRecord();
        submit.setAssignee("sam");
        when(approvalRepository.findLatest("BOM_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submit));
        when(repository.nextVersionNo(1L)).thenReturn(3);
        when(itemRepository.listByVersion(any())).thenReturn(List.of());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new BomVersionApplicationService(repository, mock(BomRepository.class), itemRepository, approvalService,
                approvalRepository, openConfirms(), clock);
    }

    @Test
    void submitThenApproveLeavesSingleActive() {
        RouteVersionSubmitRequest submit = new RouteVersionSubmitRequest();
        submit.setAssignee("sam");
        service.submit(1L, 11L, "lee", submit);

        RouteVersionDecisionRequest decision = new RouteVersionDecisionRequest();
        decision.setOperator("sam");
        service.approve(1L, 11L, decision);

        assertThat(rows.stream().filter(row -> "ACTIVE".equals(row.getStatus()))).hasSize(1);
        assertThat(rows.get(1).getStatus()).isEqualTo("ACTIVE");
        assertThat(rows.get(1).getEffectiveAt()).isEqualTo(LocalDateTime.of(2026, 10, 16, 12, 0));
        assertThat(rows.get(0).getStatus()).isEqualTo("INACTIVE");
        verify(approvalService).record(eq("BOM_VERSION"), eq(11L), eq("APPROVE"), eq("PENDING"), eq("ACTIVE"), eq(null),
                eq("sam"), eq(null));
    }

    @Test
    void copyCarriesItems() {
        rows.get(0).setRemark("量产");
        BomItem source = new BomItem();
        source.setLineNo(1);
        source.setMaterialId(8L);
        source.setQtyPer(new BigDecimal("1.5"));
        source.setStepId(7L);
        source.setRouteSeqNo(10);
        source.setSubstituteGroup("G1");
        when(itemRepository.listByVersion(10L)).thenReturn(List.of(source));
        doAnswer(invocation -> {
            BomVersion saved = invocation.getArgument(0);
            saved.setId(20L);
            rows.add(saved);
            return null;
        }).when(repository).save(any());

        service.copy(1L, 10L);

        assertThat(rows.get(2).getStatus()).isEqualTo("DRAFT");
        assertThat(rows.get(2).getVersionNo()).isEqualTo(3);
        assertThat(rows.get(2).getCopyFromVersionId()).isEqualTo(10L);
        assertThat(rows.get(2).getRemark()).isEqualTo("量产");
        verify(itemRepository).save(org.mockito.ArgumentMatchers.<BomItem>argThat(item -> item.getBomVersionId().equals(20L)
                && item.getLineNo().equals(1) && item.getMaterialId().equals(8L) && item.getRouteSeqNo().equals(10)
                && "G1".equals(item.getSubstituteGroup())));
    }

    @Test
    void approveRejectsOtherOperator() {
        rows.get(1).setStatus("PENDING");
        RouteVersionDecisionRequest decision = new RouteVersionDecisionRequest();
        decision.setOperator("lee");

        assertThatThrownBy(() -> service.approve(1L, 11L, decision))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE);
    }

    private static BomVersion version(Long id, String status) {
        BomVersion version = new BomVersion();
        version.setId(id);
        version.setBomId(1L);
        version.setStatus(status);
        return version;
    }

    private static ConfirmGateApplicationService openConfirms() {
        ConfirmGateApplicationService confirms = mock(ConfirmGateApplicationService.class);
        when(confirms.allowed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        return confirms;
    }
}
