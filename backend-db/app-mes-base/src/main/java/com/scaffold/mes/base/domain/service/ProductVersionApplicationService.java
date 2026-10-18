package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ProductVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.entity.ProductVersion;
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

import lombok.RequiredArgsConstructor;

/**
 * ProductVersionApplicationService 产品版本应用服务
 * <p>
 * 绑定 Route 和 BOM 主数据。生效前两者都必须已有 ACTIVE 版本。通过前先锁产品主表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ProductVersionApplicationService {

    static final String BIZ_TYPE = "PRODUCT_VERSION";
    private static final String DRAFT = "DRAFT";
    private static final String PENDING = "PENDING";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final ProductVersionRepository repository;
    private final ProductRepository productRepository;
    private final RouteVersionRepository routeVersionRepository;
    private final BomVersionRepository bomVersionRepository;
    private final RouteStepRepository routeStepRepository;
    private final BomItemRepository bomItemRepository;
    private final ApprovalRecordApplicationService approvalService;
    private final ApprovalRecordRepository approvalRepository;
    private final ConfirmGateApplicationService confirms;
    private final Clock clock;

    /**
     * 分页查询某个产品的版本。
     *
     * @param productId 产品
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    /**
     * 下工单前固化当前生效的 Route 和 BOM 版本。
     *
     * @param productVersionId 产品版本，必须是 ACTIVE
     * @return 固化结果
     * @throws BusinessException 版本不存在、未生效，或 BOM 与 Route 不一致
     */
    public FrozenProcess freeze(Long productVersionId) {
        ProductVersion version = repository.findById(productVersionId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_FOUND, "产品版本不存在"));
        if (!ACTIVE.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_MASTER_INACTIVE, "只有生效的产品版本可以下工单");
        }
        requireActiveMasters(version);
        Product product = productRepository.findById(version.getProductId())
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在"));
        Long routeVersionId = routeVersionRepository.listActive(version.getRouteId()).get(0).getId();
        Long bomVersionId = bomVersionRepository.listActive(version.getBomId()).get(0).getId();
        return new FrozenProcess(product.getId(), version.getId(), routeVersionId, bomVersionId, product.getCustomerId());
    }

    public PageDTO<ProductVersion> page(Long productId, int page, int pageSize) {
        if (productRepository.findById(productId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
        }
        return repository.page(productId, page, pageSize);
    }

    /**
     * 新增草稿。
     *
     * @param productId 产品
     * @param request 绑定和备注
     */
    @Transactional
    public void create(Long productId, ProductVersionSaveRequest request) {
        productRepository.lock(productId);
        ProductVersion version = new ProductVersion();
        version.setProductId(productId);
        version.setVersionNo(repository.nextVersionNo(productId));
        version.setStatus(DRAFT);
        version.setRouteId(request.getRouteId());
        version.setBomId(request.getBomId());
        version.setRemark(request.getRemark());
        repository.save(version);
    }

    /**
     * 修改草稿的绑定和备注。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 绑定和备注
     * @throws BusinessException 版本不存在或不是草稿
     */
    @Transactional
    public void update(Long productId, Long id, ProductVersionSaveRequest request) {
        ProductVersion version = requireDraft(productId, id);
        version.setRouteId(request.getRouteId());
        version.setBomId(request.getBomId());
        version.setRemark(request.getRemark());
        repository.update(version);
    }

    /**
     * 复制成新草稿。只复制 Route、BOM 绑定和备注。
     *
     * @param productId 产品
     * @param id 来源版本
     */
    @Transactional
    public void copy(Long productId, Long id) {
        ProductVersion source = require(productId, id);
        productRepository.lock(productId);
        ProductVersion copy = new ProductVersion();
        copy.setProductId(productId);
        copy.setVersionNo(repository.nextVersionNo(productId));
        copy.setStatus(DRAFT);
        copy.setRouteId(source.getRouteId());
        copy.setBomId(source.getBomId());
        copy.setCopyFromVersionId(source.getId());
        copy.setRemark(source.getRemark());
        repository.save(copy);
    }

    /**
     * 提交审批。Route 和 BOM 此时必须都已指定，且 BOM 行与当前生效 Route 对齐。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param operator 提交人
     * @param request 审批人和意见
     * @throws BusinessException 不是草稿，或没绑 Route、BOM
     */
    @Transactional
    public void submit(Long productId, Long id, String operator, RouteVersionSubmitRequest request) {
        ProductVersion version = requireDraft(productId, id);
        requireActiveMasters(version);
        if (request.getAssignee() == null || request.getAssignee().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_ASSIGNEE_REQUIRED, "提交审批必须指定审批人");
        }
        version.setStatus(PENDING);
        version.setSubmittedBy(operator);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "SUBMIT", DRAFT, PENDING, request.getAssignee(), operator, request.getComment());
    }

    /**
     * 审批通过并生效。再次确认 Route 和 BOM 当前有 ACTIVE 版本。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，操作人不是指定审批人，或主数据没有 ACTIVE 版本
     */
    @Transactional
    public void approve(Long productId, Long id, RouteVersionDecisionRequest request) {
        if (!confirms.allowed("VERSION_ACTIVATE", "PRODUCT_VERSION", id)) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_REQUIRED, "版本生效还没有完成确认");
        }
        productRepository.lock(productId);
        ProductVersion version = requirePending(productId, id);
        requireAssignee(id, request.getOperator());
        requireActiveMasters(version);
        LocalDateTime now = LocalDateTime.now(clock);
        for (ProductVersion active : repository.listActive(productId)) {
            active.setStatus(INACTIVE);
            active.setExpireAt(now);
            repository.update(active);
        }
        version.setStatus(ACTIVE);
        version.setEffectiveAt(now);
        version.setExpireAt(null);
        version.setApprovedBy(request.getOperator());
        version.setApprovedAt(now);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "APPROVE", PENDING, ACTIVE, null, request.getOperator(), request.getComment());
    }

    /**
     * 驳回为草稿。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 审批人和意见
     * @throws BusinessException 不是待审批，或操作人不是指定审批人
     */
    @Transactional
    public void reject(Long productId, Long id, RouteVersionDecisionRequest request) {
        ProductVersion version = requirePending(productId, id);
        requireAssignee(id, request.getOperator());
        version.setStatus(DRAFT);
        version.setSubmittedBy(null);
        repository.update(version);
        approvalService.record(BIZ_TYPE, id, "REJECT", PENDING, DRAFT, null, request.getOperator(), request.getComment());
    }

    private void requireMasters(ProductVersion version) {
        if (version.getRouteId() == null || version.getBomId() == null) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_MASTER_INACTIVE, "生效前必须绑定 Route 和 BOM");
        }
    }

    private void requireActiveMasters(ProductVersion version) {
        requireMasters(version);
        List<RouteVersion> routes = routeVersionRepository.listActive(version.getRouteId());
        List<BomVersion> boms = bomVersionRepository.listActive(version.getBomId());
        if (routes.isEmpty() || boms.isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_MASTER_INACTIVE, "Route 和 BOM 都必须已有生效版本");
        }
        precheck(routes.get(0).getId(), boms.get(0).getId());
    }

    /**
     * BOM 行的站点必须出现在生效 Route 中。序号不为 0 时，该序号上的站点也必须一致。
     */
    private void precheck(Long routeVersionId, Long bomVersionId) {
        Optional<Integer> line = BomRouteAlignment.firstMismatch(routeStepRepository.listByVersion(routeVersionId),
                bomItemRepository.listByVersion(bomVersionId));
        if (line.isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_BOM_MISMATCH,
                    "BOM 行与当前生效 Route 不一致，行号 " + line.get());
        }
    }

    private void requireAssignee(Long versionId, String operator) {
        ApprovalRecord submit = approvalRepository.findLatest(BIZ_TYPE, versionId, "SUBMIT")
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_PENDING, "没有待审批的提交记录"));
        if (submit.getAssignee() == null || !submit.getAssignee().equals(operator)) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_ASSIGNEE, "只有指定审批人可以通过或驳回");
        }
    }

    private ProductVersion requireDraft(Long productId, Long id) {
        ProductVersion version = require(productId, id);
        if (!DRAFT.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_DRAFT, "只有草稿可以修改或提交");
        }
        return version;
    }

    private ProductVersion requirePending(Long productId, Long id) {
        ProductVersion version = require(productId, id);
        if (!PENDING.equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_PENDING, "只有待审批版本可以审批");
        }
        return version;
    }

    private ProductVersion require(Long productId, Long id) {
        ProductVersion version = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_FOUND, "产品版本不存在"));
        if (!productId.equals(version.getProductId())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_VERSION_NOT_FOUND, "产品版本不存在");
        }
        return version;
    }
}
