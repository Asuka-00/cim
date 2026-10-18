package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.StepGroup;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * StepGroupApplicationService 站点组应用服务
 * <p>
 * 编码在租户内唯一。被站点引用时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepGroupApplicationService {

    private final StepGroupRepository repository;

    /**
     * 分页查询站点组。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param groupCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<StepGroup> page(int page, int pageSize, String groupCode) {
        return repository.page(page, pageSize, groupCode);
    }

    /**
     * 新增站点组。
     *
     * @param request 站点组内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(StepGroupSaveRequest request) {
        if (repository.findByCode(request.getGroupCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_GROUP_CODE_EXISTS, "站点组编码已存在: " + request.getGroupCode());
        }
        StepGroup group = new StepGroup();
        copy(request, group);
        repository.save(group);
    }

    /**
     * 修改站点组。
     *
     * @param id 主键
     * @param request 站点组内容
     * @throws BusinessException 站点组不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, StepGroupSaveRequest request) {
        StepGroup group = require(id);
        repository.findByCode(request.getGroupCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.STEP_GROUP_CODE_EXISTS, "站点组编码已存在: " + request.getGroupCode());
                });
        copy(request, group);
        repository.update(group);
    }

    /**
     * 删除站点组。仍被站点引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 站点组不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.STEP_GROUP_IN_USE, "站点组",
                DeleteReferenceUtils.reference("站点", used));
        repository.deleteById(id);
    }

    private StepGroup require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_GROUP_NOT_FOUND, "站点组不存在"));
    }

    private static void copy(StepGroupSaveRequest request, StepGroup group) {
        group.setGroupCode(request.getGroupCode());
        group.setGroupName(request.getGroupName());
        group.setRemark(request.getRemark());
        group.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
