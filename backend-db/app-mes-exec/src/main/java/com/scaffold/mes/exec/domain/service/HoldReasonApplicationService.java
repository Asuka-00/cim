package com.scaffold.mes.exec.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.HoldReasonSaveRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * HoldReasonApplicationService Hold 原因应用服务
 * <p>
 * 原因码在租户内唯一。被 Lot Hold 引用时不能删除，只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class HoldReasonApplicationService {

    private static final Set<String> TYPES = Set.of(
            "QUALITY", "EQUIPMENT", "PROCESS", "ENGINEERING", "MATERIAL", "OTHER");

    private final HoldReasonRepository repository;

    /**
     * 分页查询原因码。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param reasonCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<HoldReason> page(int page, int pageSize, String reasonCode) {
        return repository.page(page, pageSize, reasonCode);
    }

    /**
     * 新增原因码。
     *
     * @param request 原因内容
     * @throws BusinessException 编码已存在或类别不合法
     */
    @Transactional
    public void create(HoldReasonSaveRequest request) {
        if (repository.findByCode(request.getReasonCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.HOLD_REASON_CODE_EXISTS, "Hold 原因码已存在: " + request.getReasonCode());
        }
        HoldReason reason = new HoldReason();
        copy(request, reason);
        repository.save(reason);
    }

    /**
     * 修改原因码。
     *
     * @param id 主键
     * @param request 原因内容
     * @throws BusinessException 原因不存在、编码冲突或类别不合法
     */
    @Transactional
    public void update(Long id, HoldReasonSaveRequest request) {
        HoldReason reason = require(id);
        repository.findByCode(request.getReasonCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesExecErrorCodes.HOLD_REASON_CODE_EXISTS,
                            "Hold 原因码已存在: " + request.getReasonCode());
                });
        copy(request, reason);
        repository.update(reason);
    }

    /**
     * 删除原因码。仍被 Lot Hold 引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 原因不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        HoldReason reason = require(id);
        long used = repository.countHolds(reason.getReasonCode());
        DeleteReferenceUtils.requireNoReferences(MesExecErrorCodes.HOLD_REASON_IN_USE, "Hold 原因",
                DeleteReferenceUtils.reference("Lot Hold", used));
        repository.deleteById(id);
    }

    private HoldReason require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在"));
    }

    private static void copy(HoldReasonSaveRequest request, HoldReason reason) {
        String type = request.getHoldType() == null || request.getHoldType().isBlank() ? "OTHER" : request.getHoldType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesExecErrorCodes.HOLD_REASON_TYPE_INVALID, "Hold 类别不合法: " + type);
        }
        reason.setReasonCode(request.getReasonCode());
        reason.setReasonName(request.getReasonName());
        reason.setHoldType(type);
        reason.setReleaseRoleCode(blankToNull(request.getReleaseRoleCode()));
        reason.setRemark(request.getRemark());
        reason.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
