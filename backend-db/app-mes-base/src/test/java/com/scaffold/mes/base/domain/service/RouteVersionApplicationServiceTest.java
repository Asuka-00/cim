package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.core.domain.service.alert.MesAlertApplicationService;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.entity.ProductVersion;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.domain.repository.ProductVersionRepository;
import com.scaffold.mes.base.domain.repository.RouteRepository;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 草稿提交后由指定审批人生效。旧 ACTIVE 同时失效，同时只留一个 ACTIVE。
 */
class RouteVersionApplicationServiceTest {

    private final List<RouteVersion> rows = new ArrayList<>();
    private ConfirmGateApplicationService confirms;
    private RouteVersionRepository repository;
    private RouteStepRepository stepRepository;
    private RouteTimeLinkRepository timeLinkRepository;
    private RouteStepRecipeRepository stepRecipeRepository;
    private RouteStepSamplingRepository stepSamplingRepository;
    private RouteStepCollectItemRepository stepCollectRepository;
    private StepRepository stationRepository;
    private StepEquipmentGroupRepository equipmentGroupRepository;
    private ProductVersionRepository productVersionRepository;
    private BomVersionRepository bomVersionRepository;
    private BomItemRepository bomItemRepository;
    private MesAlertApplicationService alertService;
    private ApprovalRecordRepository approvalRepository;
    private ApprovalRecordApplicationService approvalService;
    private RouteVersionApplicationService service;

    @BeforeEach
    void setUp() {
        rows.add(version(10L, 1, "ACTIVE"));
        rows.add(version(11L, 2, "DRAFT"));
        repository = mock(RouteVersionRepository.class);
        RouteRepository routeRepository = mock(RouteRepository.class);
        approvalRepository = mock(ApprovalRecordRepository.class);
        approvalService = mock(ApprovalRecordApplicationService.class);
        when(repository.findById(any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(repository.listActive(1L)).thenAnswer(invocation -> rows.stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .toList());
        when(repository.nextVersionNo(1L)).thenReturn(3);
        stepRepository = mock(RouteStepRepository.class);
        timeLinkRepository = mock(RouteTimeLinkRepository.class);
        stepRecipeRepository = mock(RouteStepRecipeRepository.class);
        when(stepRepository.listByVersion(any())).thenReturn(List.of());
        when(timeLinkRepository.listByVersion(any())).thenReturn(List.of());
        when(stepRecipeRepository.listByVersion(any())).thenReturn(List.of());
        stepSamplingRepository = mock(RouteStepSamplingRepository.class);
        stepCollectRepository = mock(RouteStepCollectItemRepository.class);
        when(stepSamplingRepository.listByVersion(any())).thenReturn(List.of());
        when(stepCollectRepository.listByVersion(any())).thenReturn(List.of());
        stationRepository = mock(StepRepository.class);
        equipmentGroupRepository = mock(StepEquipmentGroupRepository.class);
        when(equipmentGroupRepository.listByStep(any())).thenReturn(List.of());
        productVersionRepository = mock(ProductVersionRepository.class);
        bomVersionRepository = mock(BomVersionRepository.class);
        bomItemRepository = mock(BomItemRepository.class);
        alertService = mock(MesAlertApplicationService.class);
        when(productVersionRepository.listActiveByRoute(any())).thenReturn(List.of());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new RouteVersionApplicationService(repository, routeRepository, stepRepository, timeLinkRepository,
                stepRecipeRepository, stepSamplingRepository, stepCollectRepository, stationRepository, equipmentGroupRepository,
                productVersionRepository, bomVersionRepository,
                bomItemRepository, alertService, approvalService, approvalRepository, openConfirms(), clock);
    }

    @Test
    void submitRejectsEquipmentStepWithoutGroup() {
        RouteStep routeStep = new RouteStep();
        routeStep.setSeqNo(10);
        routeStep.setStepId(7L);
        when(stepRepository.listByVersion(11L)).thenReturn(List.of(routeStep));
        Step station = new Step();
        station.setId(7L);
        station.setNeedEquipment(1);
        when(stationRepository.findById(7L)).thenReturn(Optional.of(station));

        assertThatThrownBy(() -> service.submit(1L, 11L, "lee", submit("sam")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_VERSION_EQUIPMENT_REQUIRED);
        verify(repository, never()).update(any());
    }

    @Test
    void submitAllowsEquipmentStepBoundAtStation() {
        RouteStep routeStep = new RouteStep();
        routeStep.setSeqNo(10);
        routeStep.setStepId(7L);
        when(stepRepository.listByVersion(11L)).thenReturn(List.of(routeStep));
        Step station = new Step();
        station.setId(7L);
        station.setNeedEquipment(1);
        when(stationRepository.findById(7L)).thenReturn(Optional.of(station));
        when(equipmentGroupRepository.listByStep(7L)).thenReturn(List.of(new StepEquipmentGroup()));

        service.submit(1L, 11L, "lee", submit("sam"));

        assertThat(rows.get(1).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void submitMovesDraftToPending() {
        service.submit(1L, 11L, "lee", submit("sam"));

        assertThat(rows.get(1).getStatus()).isEqualTo("PENDING");
        assertThat(rows.get(1).getSubmittedBy()).isEqualTo("lee");
        verify(approvalService).record(eq("ROUTE_VERSION"), eq(11L), eq("SUBMIT"), eq("DRAFT"), eq("PENDING"), eq("sam"),
                eq("lee"), eq(null));
    }

    @Test
    void approveDemotesPreviousActive() {
        rows.get(1).setStatus("PENDING");
        when(approvalRepository.findLatest("ROUTE_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submitRecord("sam")));

        service.approve(1L, 11L, decision("sam"));

        assertThat(rows.stream().filter(row -> "ACTIVE".equals(row.getStatus()))).hasSize(1);
        assertThat(rows.get(1).getStatus()).isEqualTo("ACTIVE");
        assertThat(rows.get(1).getEffectiveAt()).isEqualTo(LocalDateTime.of(2026, 10, 16, 12, 0));
        assertThat(rows.get(0).getStatus()).isEqualTo("INACTIVE");
        assertThat(rows.get(0).getExpireAt()).isEqualTo(rows.get(1).getEffectiveAt());
        verify(approvalService).record(eq("ROUTE_VERSION"), eq(11L), eq("APPROVE"), eq("PENDING"), eq("ACTIVE"), eq(null),
                eq("sam"), eq(null));
    }

    @Test
    void missingConfirmBlocksApprove() {
        when(confirms.allowed("VERSION_ACTIVATE", "ROUTE_VERSION", 11L)).thenReturn(false);

        assertThatThrownBy(() -> service.approve(1L, 11L, decision("sam")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.CONFIRM_REQUIRED);
        verify(repository, never()).update(any());
    }

    @Test
    void approveWarnsWhenActiveBomDrifts() {
        rows.get(1).setStatus("PENDING");
        when(approvalRepository.findLatest("ROUTE_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submitRecord("sam")));
        RouteStep step = new RouteStep();
        step.setSeqNo(10);
        step.setStepId(7L);
        when(stepRepository.listByVersion(11L)).thenReturn(List.of(step));
        ProductVersion productVersion = new ProductVersion();
        productVersion.setId(30L);
        productVersion.setBomId(3L);
        when(productVersionRepository.listActiveByRoute(1L)).thenReturn(List.of(productVersion));
        BomVersion bomVersion = new BomVersion();
        bomVersion.setId(31L);
        when(bomVersionRepository.listActive(3L)).thenReturn(List.of(bomVersion));
        BomItem item = new BomItem();
        item.setLineNo(2);
        item.setStepId(8L);
        item.setRouteSeqNo(10);
        when(bomItemRepository.listByVersion(31L)).thenReturn(List.of(item));

        service.approve(1L, 11L, decision("sam"));

        assertThat(rows.get(1).getStatus()).isEqualTo("ACTIVE");
        verify(alertService).raiseSystem(eq("PRODUCT_VERSION"), eq(30L), eq("Route 与 BOM 不一致"),
                eq("产品版本 30 的 BOM 行 2 与新 Route 版本 11 不一致"));
    }

    @Test
    void approveRejectsOtherOperator() {
        rows.get(1).setStatus("PENDING");
        when(approvalRepository.findLatest("ROUTE_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submitRecord("sam")));

        assertThatThrownBy(() -> service.approve(1L, 11L, decision("lee")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE);
        verify(repository, never()).update(any());
    }

    @Test
    void updateRejectsActive() {
        assertThatThrownBy(() -> service.update(1L, 10L, new RouteVersionSaveRequest()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT);
    }

    @Test
    void copyCarriesStepsLinksAndRecipes() {
        RouteStep sourceStep = new RouteStep();
        sourceStep.setId(50L);
        sourceStep.setSeqNo(10);
        sourceStep.setStepId(7L);
        sourceStep.setStepAlias("二次曝光");
        when(stepRepository.listByVersion(10L)).thenReturn(List.of(sourceStep));
        RouteTimeLink sourceLink = new RouteTimeLink();
        sourceLink.setFromSeqNo(10);
        sourceLink.setToSeqNo(20);
        sourceLink.setMaxMinutes(30);
        when(timeLinkRepository.listByVersion(10L)).thenReturn(List.of(sourceLink));
        RouteStepRecipe sourceRecipe = new RouteStepRecipe();
        sourceRecipe.setRouteStepId(50L);
        sourceRecipe.setRecipeId(9L);
        sourceRecipe.setProductId(0L);
        sourceRecipe.setIsDefault(1);
        when(stepRecipeRepository.listByVersion(10L)).thenReturn(List.of(sourceRecipe));
        RouteStepSampling sourceSampling = new RouteStepSampling();
        sourceSampling.setRouteStepId(50L);
        sourceSampling.setProductId(0L);
        sourceSampling.setRuleId(9L);
        when(stepSamplingRepository.listByVersion(10L)).thenReturn(List.of(sourceSampling));
        RouteStepCollectItem sourceCollect = new RouteStepCollectItem();
        sourceCollect.setRouteStepId(50L);
        sourceCollect.setItemId(15L);
        sourceCollect.setProductId(0L);
        sourceCollect.setIsRequired(1);
        when(stepCollectRepository.listByVersion(10L)).thenReturn(List.of(sourceCollect));
        AtomicLong ids = new AtomicLong(100L);
        doAnswer(invocation -> {
            RouteVersion saved = invocation.getArgument(0);
            saved.setId(ids.getAndIncrement());
            rows.add(saved);
            return null;
        }).when(repository).save(any());
        doAnswer(invocation -> {
            RouteStep saved = invocation.getArgument(0);
            saved.setId(ids.getAndIncrement());
            return null;
        }).when(stepRepository).save(any());

        service.copy(1L, 10L);

        RouteVersion copied = rows.get(2);
        assertThat(copied.getStatus()).isEqualTo("DRAFT");
        assertThat(copied.getVersionNo()).isEqualTo(3);
        assertThat(copied.getCopyFromVersionId()).isEqualTo(10L);
        verify(stepRepository).save(org.mockito.ArgumentMatchers.<RouteStep>argThat(step -> step.getRouteVersionId().equals(100L)
                && step.getSeqNo().equals(10) && "二次曝光".equals(step.getStepAlias())));
        verify(timeLinkRepository).save(org.mockito.ArgumentMatchers.<RouteTimeLink>argThat(link -> link.getRouteVersionId().equals(100L)
                && link.getFromSeqNo().equals(10) && link.getMaxMinutes().equals(30)));
        verify(stepRecipeRepository).save(org.mockito.ArgumentMatchers.<RouteStepRecipe>argThat(recipe -> recipe.getRouteVersionId().equals(100L)
                && recipe.getRouteStepId().equals(101L) && recipe.getRecipeId().equals(9L)));
        verify(stepSamplingRepository).save(org.mockito.ArgumentMatchers.<RouteStepSampling>argThat(row ->
                row.getRouteVersionId().equals(100L) && row.getRouteStepId().equals(101L) && row.getRuleId().equals(9L)));
        verify(stepCollectRepository).save(org.mockito.ArgumentMatchers.<RouteStepCollectItem>argThat(row ->
                row.getRouteVersionId().equals(100L) && row.getRouteStepId().equals(101L) && row.getItemId().equals(15L)));
    }

    @Test
    void rejectReturnsToDraft() {
        rows.get(1).setStatus("PENDING");
        rows.get(1).setSubmittedBy("lee");
        when(approvalRepository.findLatest("ROUTE_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submitRecord("sam")));

        service.reject(1L, 11L, decision("sam"));

        assertThat(rows.get(1).getStatus()).isEqualTo("DRAFT");
        assertThat(rows.get(1).getSubmittedBy()).isNull();
    }

    private static RouteVersionSubmitRequest submit(String assignee) {
        RouteVersionSubmitRequest request = new RouteVersionSubmitRequest();
        request.setAssignee(assignee);
        return request;
    }

    private static RouteVersionDecisionRequest decision(String operator) {
        RouteVersionDecisionRequest request = new RouteVersionDecisionRequest();
        request.setOperator(operator);
        return request;
    }

    private static ApprovalRecord submitRecord(String assignee) {
        ApprovalRecord record = new ApprovalRecord();
        record.setAssignee(assignee);
        record.setAction("SUBMIT");
        return record;
    }

    private static RouteVersion version(Long id, int versionNo, String status) {
        RouteVersion version = new RouteVersion();
        version.setId(id);
        version.setRouteId(1L);
        version.setVersionNo(versionNo);
        version.setStatus(status);
        return version;
    }

    private ConfirmGateApplicationService openConfirms() {
        confirms = mock(ConfirmGateApplicationService.class);
        when(confirms.allowed(any(), any(), any())).thenReturn(true);
        return confirms;
    }
}
