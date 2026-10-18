package com.scaffold.mes.base.domain.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.BomItemSaveRequest;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BomItemApplicationService BOM 行应用服务
 * <p>
 * 只有草稿版本能改行。行号在版本内唯一。步骤序号 0 表示该站点每次经过都消耗。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BomItemApplicationService {

    private static final Set<String> MODES = Set.of(
            "PER_WAFER", "PER_DIE", "PER_STRIP", "PER_UNIT", "PER_LOT", "PER_RUN", "PER_TIME");
    private static final Set<String> POLICIES = Set.of("ALLOW", "WARN", "BLOCK");

    private final BomItemRepository repository;
    private final BomVersionRepository versionRepository;
    private final MaterialRepository materialRepository;
    private final UomRepository uomRepository;
    private final StepRepository stepRepository;

    /**
     * 列出某个版本的 BOM 行。
     *
     * @param bomVersionId BOM 版本
     * @return 按行号排序的行
     * @throws BusinessException 版本不存在
     */
    public List<BomItem> list(Long bomVersionId) {
        requireVersion(bomVersionId);
        return repository.listByVersion(bomVersionId);
    }

    /**
     * 在草稿版本上新增行。
     *
     * @param bomVersionId BOM 版本
     * @param request 行内容
     * @throws BusinessException 版本不是草稿，行号冲突，或用量、消耗方式不合法
     */
    @Transactional
    public void create(Long bomVersionId, BomItemSaveRequest request) {
        requireDraft(bomVersionId);
        if (repository.findByLine(bomVersionId, request.getLineNo()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_LINE_EXISTS, "BOM 行号已存在: " + request.getLineNo());
        }
        BomItem item = new BomItem();
        item.setBomVersionId(bomVersionId);
        copy(request, item);
        repository.save(item);
    }

    /**
     * 修改草稿版本上的行。
     *
     * @param bomVersionId BOM 版本
     * @param id 行主键
     * @param request 行内容
     * @throws BusinessException 行不存在，版本不是草稿，或行号与其他行冲突
     */
    @Transactional
    public void update(Long bomVersionId, Long id, BomItemSaveRequest request) {
        requireDraft(bomVersionId);
        BomItem item = require(bomVersionId, id);
        repository.findByLine(bomVersionId, request.getLineNo())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_LINE_EXISTS, "BOM 行号已存在: " + request.getLineNo());
                });
        copy(request, item);
        repository.update(item);
    }

    /**
     * 删除草稿版本上的行。
     *
     * @param bomVersionId BOM 版本
     * @param id 行主键
     * @throws BusinessException 行不存在或版本不是草稿
     */
    @Transactional
    public void delete(Long bomVersionId, Long id) {
        requireDraft(bomVersionId);
        require(bomVersionId, id);
        repository.deleteById(id);
    }

    private void copy(BomItemSaveRequest request, BomItem item) {
        if (request.getQtyPer() == null || request.getQtyPer().signum() <= 0) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_QTY_INVALID, "单位用量必须大于 0");
        }
        String mode = request.getConsumeMode() == null || request.getConsumeMode().isBlank()
                ? "PER_WAFER"
                : request.getConsumeMode();
        if (!MODES.contains(mode)) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_CONSUME_MODE_INVALID, "消耗方式不合法: " + mode);
        }
        String policy = request.getSubstitutePolicy() == null || request.getSubstitutePolicy().isBlank()
                ? "ALLOW"
                : request.getSubstitutePolicy();
        if (!POLICIES.contains(policy)) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_CONSUME_MODE_INVALID, "替代策略不合法: " + policy);
        }
        int routeSeq = request.getRouteSeqNo() == null ? 0 : request.getRouteSeqNo();
        if (routeSeq < 0) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_QTY_INVALID, "步骤序号不能为负");
        }
        if (materialRepository.findById(request.getMaterialId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.MATERIAL_NOT_FOUND, "物料不存在");
        }
        if (uomRepository.findById(request.getUomId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.UOM_NOT_FOUND, "物料计量单位不存在");
        }
        if (stepRepository.findById(request.getStepId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在");
        }
        item.setLineNo(request.getLineNo());
        item.setMaterialId(request.getMaterialId());
        item.setQtyPer(request.getQtyPer());
        item.setUomId(request.getUomId());
        item.setScrapRate(request.getScrapRate() == null ? BigDecimal.ZERO : request.getScrapRate());
        item.setConsumeMode(mode);
        item.setStepId(request.getStepId());
        item.setRouteSeqNo(routeSeq);
        item.setBackflush(request.getBackflush() == null ? 1 : request.getBackflush());
        item.setIsCritical(request.getIsCritical());
        item.setSubstituteGroup(request.getSubstituteGroup());
        item.setSubstitutePriority(request.getSubstitutePriority());
        item.setSubstitutePolicy(policy);
        item.setRemark(request.getRemark());
    }

    private BomVersion requireDraft(Long bomVersionId) {
        BomVersion version = requireVersion(bomVersionId);
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_DRAFT, "只有草稿版本可以修改 BOM 行");
        }
        return version;
    }

    private BomVersion requireVersion(Long bomVersionId) {
        return versionRepository.findById(bomVersionId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.BOM_VERSION_NOT_FOUND, "BOM 版本不存在"));
    }

    private BomItem require(Long bomVersionId, Long id) {
        BomItem item = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.BOM_ITEM_NOT_FOUND, "BOM 行不存在"));
        if (!bomVersionId.equals(item.getBomVersionId())) {
            throw new BusinessException(MesBaseErrorCodes.BOM_ITEM_NOT_FOUND, "BOM 行不存在");
        }
        return item;
    }
}
