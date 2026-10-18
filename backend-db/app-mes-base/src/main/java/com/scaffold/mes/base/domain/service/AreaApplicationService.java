package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.AreaSaveRequest;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * AreaApplicationService 区域应用服务
 * <p>
 * 编码在租户内唯一。上级区域不能指向自己或形成循环。被下级、站点或设备引用时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class AreaApplicationService {

    private final AreaRepository repository;

    /**
     * 分页查询区域。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param areaCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Area> page(int page, int pageSize, String areaCode) {
        return repository.page(page, pageSize, areaCode);
    }

    /**
     * 新增区域。
     *
     * @param request 区域内容
     * @throws BusinessException 编码已存在，或上级区域不存在
     */
    @Transactional
    public void create(AreaSaveRequest request) {
        if (repository.findByCode(request.getAreaCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.AREA_CODE_EXISTS, "区域编码已存在: " + request.getAreaCode());
        }
        requireParent(null, request.getParentId());
        Area area = new Area();
        copy(request, area);
        repository.save(area);
    }

    /**
     * 修改区域。
     *
     * @param id 主键
     * @param request 区域内容
     * @throws BusinessException 区域不存在、编码冲突，或上级区域形成循环
     */
    @Transactional
    public void update(Long id, AreaSaveRequest request) {
        Area area = require(id);
        repository.findByCode(request.getAreaCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.AREA_CODE_EXISTS, "区域编码已存在: " + request.getAreaCode());
                });
        requireParent(id, request.getParentId());
        copy(request, area);
        repository.update(area);
    }

    /**
     * 删除区域。仍被引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 区域不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.AREA_IN_USE, "区域",
                DeleteReferenceUtils.reference("下级区域、站点或设备", used));
        repository.deleteById(id);
    }

    private Area require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.AREA_NOT_FOUND, "区域不存在"));
    }

    /**
     * 沿上级链向上走。走到当前区域，或链长超过库内合理层数，都视为循环。
     *
     * @param id 正在修改的区域，新增时为空
     * @param parentId 上级区域，空表示顶层
     */
    private void requireParent(Long id, Long parentId) {
        Long cursor = parentId;
        int depth = 0;
        while (cursor != null) {
            if (cursor.equals(id)) {
                throw new BusinessException(MesBaseErrorCodes.AREA_PARENT_CYCLE, "上级区域不能形成循环");
            }
            Area parent = repository.findById(cursor)
                    .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.AREA_NOT_FOUND, "上级区域不存在"));
            cursor = parent.getParentId();
            if (++depth > 32) {
                throw new BusinessException(MesBaseErrorCodes.AREA_PARENT_CYCLE, "上级区域不能形成循环");
            }
        }
    }

    private static void copy(AreaSaveRequest request, Area area) {
        area.setAreaCode(request.getAreaCode());
        area.setAreaName(request.getAreaName());
        area.setParentId(request.getParentId());
        area.setRemark(request.getRemark());
        area.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
