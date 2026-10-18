package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.CapabilityDecision;
import com.scaffold.mes.base.domain.dto.EquipmentCapabilitySaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentCapability;
import com.scaffold.mes.base.domain.repository.EquipmentCapabilityRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentCapabilityApplicationService 设备能力应用服务
 * <p>
 * 最具体的 Recipe、产品组合优先。任一命中层是停用、吊销或过期，就不能被更泛的有效资格放过。
 * 设备加站点完全没有记录时，才改看站点设备组绑定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentCapabilityApplicationService {

    private static final long ANY = 0L;
    private static final Set<String> STATUSES = Set.of("VALID", "SUSPENDED", "EXPIRED", "REVOKED");
    private static final Set<String> BLOCKING = Set.of("SUSPENDED", "EXPIRED", "REVOKED");

    private final EquipmentCapabilityRepository repository;
    private final EquipmentRepository equipmentRepository;
    private final StepRepository stepRepository;
    private final Clock clock;

    /**
     * 判定这台设备能否跑这个站点、Recipe 和产品。
     *
     * @param equipmentId 设备
     * @param stepId 站点
     * @param recipeId Recipe，空按通用
     * @param productId 产品，空按通用
     * @param groupBound 设备所属组是否已绑在该站点上。没有能力记录时用它决定放行
     * @return 判定结果
     */
    /**
     * 列出某台设备的能力。
     *
     * @param equipmentId 设备
     * @return 能力列表
     * @throws BusinessException 设备不存在
     */
    public List<EquipmentCapability> list(Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在");
        }
        return repository.listByEquipment(equipmentId);
    }

    public CapabilityDecision decide(Long equipmentId, Long stepId, Long recipeId, Long productId, boolean groupBound) {
        long recipe = recipeId == null ? ANY : recipeId;
        long product = productId == null ? ANY : productId;
        List<EquipmentCapability> all = repository.listByEquipmentAndStep(equipmentId, stepId);
        if (all.isEmpty()) {
            return groupBound ? new CapabilityDecision(true, "GROUP") : new CapabilityDecision(false, "NO_CAPABILITY");
        }
        List<EquipmentCapability> matched = all.stream().filter(row -> matches(row, recipe, product)).toList();
        if (matched.isEmpty()) {
            return new CapabilityDecision(false, "NO_MATCH");
        }
        if (matched.stream().anyMatch(row -> BLOCKING.contains(row.getStatus()))) {
            return new CapabilityDecision(false, "INHIBIT");
        }
        EquipmentCapability best = matched.stream().max(Comparator.comparingInt(row -> specificity(row))).orElseThrow();
        if (!withinWindow(best, LocalDateTime.now(clock))) {
            return new CapabilityDecision(false, "OUT_OF_WINDOW");
        }
        return new CapabilityDecision(true, "MATCH");
    }

    /**
     * 把已过有效期的 VALID 记录改成 EXPIRED。
     *
     * @return 更新条数
     */
    @Transactional
    public int expireDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<EquipmentCapability> due = repository.listDue(now);
        for (EquipmentCapability capability : due) {
            capability.setStatus("EXPIRED");
            repository.update(capability);
        }
        return due.size();
    }

    /**
     * 新增能力。
     *
     * @param request 能力内容
     * @throws BusinessException 主数据不存在、状态非法或组合已存在
     */
    @Transactional
    public void create(EquipmentCapabilitySaveRequest request) {
        long recipe = request.getRecipeId() == null ? ANY : request.getRecipeId();
        long product = request.getProductId() == null ? ANY : request.getProductId();
        requireRefs(request);
        if (repository.findExact(request.getEquipmentId(), request.getStepId(), recipe, product).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_EXISTS, "设备能力已存在");
        }
        EquipmentCapability capability = new EquipmentCapability();
        copy(request, capability, recipe, product);
        repository.save(capability);
    }

    /**
     * 修改能力。设备、站点、Recipe、产品组合不能和另一条重复。
     *
     * @param id 主键
     * @param request 能力内容
     * @throws BusinessException 记录不存在、主数据不存在、状态非法或组合已存在
     */
    @Transactional
    public void update(Long id, EquipmentCapabilitySaveRequest request) {
        EquipmentCapability capability = require(id);
        long recipe = request.getRecipeId() == null ? ANY : request.getRecipeId();
        long product = request.getProductId() == null ? ANY : request.getProductId();
        requireRefs(request);
        repository.findExact(request.getEquipmentId(), request.getStepId(), recipe, product)
                .filter(other -> !id.equals(other.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_EXISTS, "设备能力已存在");
                });
        copy(request, capability, recipe, product);
        repository.update(capability);
    }

    /**
     * 删除能力。
     *
     * @param id 主键
     * @throws BusinessException 记录不存在
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        repository.deleteById(id);
    }

    private EquipmentCapability require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_NOT_FOUND, "设备能力不存在"));
    }

    private boolean matches(EquipmentCapability row, long recipe, long product) {
        boolean recipeOk = row.getRecipeId() == ANY || row.getRecipeId() == recipe;
        boolean productOk = row.getProductId() == ANY || row.getProductId() == product;
        return recipeOk && productOk;
    }

    private int specificity(EquipmentCapability row) {
        int score = 0;
        if (row.getRecipeId() != null && row.getRecipeId() != ANY) {
            score += 2;
        }
        if (row.getProductId() != null && row.getProductId() != ANY) {
            score += 1;
        }
        return score;
    }

    private boolean withinWindow(EquipmentCapability row, LocalDateTime now) {
        if (row.getValidFrom() != null && now.isBefore(row.getValidFrom())) {
            return false;
        }
        return row.getValidTo() == null || !now.isAfter(row.getValidTo());
    }

    private void copy(EquipmentCapabilitySaveRequest request, EquipmentCapability capability, long recipe, long product) {
        String status = request.getStatus() == null || request.getStatus().isBlank() ? "VALID" : request.getStatus();
        if (!STATUSES.contains(status)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_STATUS_INVALID, "能力状态不合法: " + status);
        }
        if (request.getValidFrom() != null && request.getValidTo() != null && request.getValidFrom().isAfter(request.getValidTo())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_STATUS_INVALID, "资格开始不能晚于结束");
        }
        capability.setEquipmentId(request.getEquipmentId());
        capability.setStepId(request.getStepId());
        capability.setRecipeId(recipe);
        capability.setProductId(product);
        capability.setStatus(status);
        capability.setValidFrom(request.getValidFrom());
        capability.setValidTo(request.getValidTo());
        capability.setQualifiedBy(request.getQualifiedBy());
        capability.setRemark(request.getRemark());
    }

    private void requireRefs(EquipmentCapabilitySaveRequest request) {
        if (equipmentRepository.findById(request.getEquipmentId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在");
        }
        if (stepRepository.findById(request.getStepId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在");
        }
    }
}
