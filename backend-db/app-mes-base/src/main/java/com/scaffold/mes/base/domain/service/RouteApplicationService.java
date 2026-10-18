package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteSaveRequest;
import com.scaffold.mes.base.domain.entity.Route;
import com.scaffold.mes.base.domain.repository.RouteRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * RouteApplicationService Route 应用服务
 * <p>
 * 编码在租户内唯一。已有版本时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteApplicationService {

    private final RouteRepository repository;

    /**
     * 分页查询 Route。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param routeCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Route> page(int page, int pageSize, String routeCode) {
        return repository.page(page, pageSize, routeCode);
    }

    /**
     * 新增 Route。
     *
     * @param request Route 内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(RouteSaveRequest request) {
        if (repository.findByCode(request.getRouteCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_CODE_EXISTS, "Route 编码已存在: " + request.getRouteCode());
        }
        Route route = new Route();
        copy(request, route);
        repository.save(route);
    }

    /**
     * 修改 Route。
     *
     * @param id 主键
     * @param request Route 内容
     * @throws BusinessException Route 不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, RouteSaveRequest request) {
        Route route = require(id);
        repository.findByCode(request.getRouteCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.ROUTE_CODE_EXISTS, "Route 编码已存在: " + request.getRouteCode());
                });
        copy(request, route);
        repository.update(route);
    }

    /**
     * 删除 Route。仍有版本时拒绝。
     *
     * @param id 主键
     * @throws BusinessException Route 不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.ROUTE_IN_USE, "Route",
                DeleteReferenceUtils.reference("版本", used));
        repository.deleteById(id);
    }

    private Route require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_NOT_FOUND, "Route 不存在"));
    }

    private static void copy(RouteSaveRequest request, Route route) {
        route.setRouteCode(request.getRouteCode());
        route.setRouteName(request.getRouteName());
        route.setDescription(request.getDescription());
        route.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
