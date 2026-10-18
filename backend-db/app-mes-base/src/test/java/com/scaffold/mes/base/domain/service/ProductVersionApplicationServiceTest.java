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
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.entity.ProductVersion;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.ProductVersionRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.response.FrozenProcess;

/**
 * 产品版本生效前，绑定的 Route 和 BOM 都必须已有 ACTIVE 版本。
 */
class ProductVersionApplicationServiceTest {

    private final List<ProductVersion> rows = new ArrayList<>();
    private RouteVersionRepository routeVersionRepository;
    private BomVersionRepository bomVersionRepository;
    private RouteStepRepository routeStepRepository;
    private BomItemRepository bomItemRepository;
    private ProductVersionRepository repository;
    private ProductRepository productRepository;
    private ProductVersionApplicationService service;

    @BeforeEach
    void setUp() {
        rows.add(version(11L, "DRAFT"));
        repository = mock(ProductVersionRepository.class);
        routeVersionRepository = mock(RouteVersionRepository.class);
        bomVersionRepository = mock(BomVersionRepository.class);
        ApprovalRecordRepository approvalRepository = mock(ApprovalRecordRepository.class);
        when(repository.findById(any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(repository.listActive(1L)).thenAnswer(invocation -> rows.stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .toList());
        ApprovalRecord submit = new ApprovalRecord();
        submit.setAssignee("sam");
        when(approvalRepository.findLatest("PRODUCT_VERSION", 11L, "SUBMIT")).thenReturn(Optional.of(submit));
        RouteVersion routeVersion = new RouteVersion();
        routeVersion.setId(21L);
        BomVersion bomVersion = new BomVersion();
        bomVersion.setId(31L);
        when(routeVersionRepository.listActive(2L)).thenReturn(List.of(routeVersion));
        when(bomVersionRepository.listActive(3L)).thenReturn(List.of(bomVersion));
        RouteStepRepository routeStepRepository = mock(RouteStepRepository.class);
        BomItemRepository bomItemRepository = mock(BomItemRepository.class);
        when(routeStepRepository.listByVersion(21L)).thenReturn(List.of());
        when(bomItemRepository.listByVersion(31L)).thenReturn(List.of());
        productRepository = mock(ProductRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new ProductVersionApplicationService(repository, productRepository, routeVersionRepository,
                bomVersionRepository, routeStepRepository, bomItemRepository, mock(ApprovalRecordApplicationService.class),
                approvalRepository, openConfirms(), clock);
        this.routeStepRepository = routeStepRepository;
        this.bomItemRepository = bomItemRepository;
    }

    @Test
    void freezeCapturesActiveRouteAndBom() {
        rows.get(0).setStatus("ACTIVE");
        rows.get(0).setRouteId(2L);
        rows.get(0).setBomId(3L);
        Product product = new Product();
        product.setId(1L);
        product.setCustomerId(8L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        RouteStep step = new RouteStep();
        step.setSeqNo(10);
        step.setStepId(7L);
        when(routeStepRepository.listByVersion(21L)).thenReturn(List.of(step));
        BomItem item = new BomItem();
        item.setLineNo(1);
        item.setStepId(7L);
        item.setRouteSeqNo(10);
        when(bomItemRepository.listByVersion(31L)).thenReturn(List.of(item));

        FrozenProcess frozen = service.freeze(11L);

        assertThat(frozen.routeVersionId()).isEqualTo(21L);
        assertThat(frozen.bomVersionId()).isEqualTo(31L);
        assertThat(frozen.customerId()).isEqualTo(8L);
    }

    @Test
    void approveRequiresActiveRouteAndBom() {
        rows.get(0).setStatus("PENDING");
        rows.get(0).setRouteId(2L);
        rows.get(0).setBomId(3L);
        when(routeVersionRepository.listActive(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.approve(1L, 11L, decision("sam")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_VERSION_MASTER_INACTIVE);
        verify(repository, never()).update(any());
    }

    @Test
    void approveActivatesWhenMastersAreActive() {
        rows.get(0).setStatus("PENDING");
        rows.get(0).setRouteId(2L);
        rows.get(0).setBomId(3L);

        service.approve(1L, 11L, decision("sam"));

        assertThat(rows.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(rows.get(0).getRouteId()).isEqualTo(2L);
        assertThat(rows.get(0).getBomId()).isEqualTo(3L);
    }

    @Test
    void submitRejectsBomStepMissingFromRoute() {
        rows.get(0).setRouteId(2L);
        rows.get(0).setBomId(3L);
        RouteStep step = new RouteStep();
        step.setSeqNo(10);
        step.setStepId(7L);
        when(routeStepRepository.listByVersion(21L)).thenReturn(List.of(step));
        BomItem item = new BomItem();
        item.setLineNo(1);
        item.setStepId(8L);
        item.setRouteSeqNo(10);
        when(bomItemRepository.listByVersion(31L)).thenReturn(List.of(item));
        RouteVersionSubmitRequest submit = new RouteVersionSubmitRequest();
        submit.setAssignee("sam");

        assertThatThrownBy(() -> service.submit(1L, 11L, "lee", submit))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_VERSION_BOM_MISMATCH);
    }

    @Test
    void submitRejectsMissingMasters() {
        RouteVersionSubmitRequest submit = new RouteVersionSubmitRequest();
        submit.setAssignee("sam");

        assertThatThrownBy(() -> service.submit(1L, 11L, "lee", submit))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_VERSION_MASTER_INACTIVE);
    }

    private static RouteVersionDecisionRequest decision(String operator) {
        RouteVersionDecisionRequest request = new RouteVersionDecisionRequest();
        request.setOperator(operator);
        return request;
    }

    private static ProductVersion version(Long id, String status) {
        ProductVersion version = new ProductVersion();
        version.setId(id);
        version.setProductId(1L);
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
