package com.scaffold.mes.base.infra.repository.db;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.Route;
import com.scaffold.mes.base.domain.repository.RouteRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteDbRepositoryImpl Route 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_route。生效前用 FOR UPDATE NOWAIT 锁主表行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteDbRepositoryImpl implements RouteRepository {

    private final RouteMapper mapper;

    private DbRepositorySupport<Route> support() {
        return new DbRepositorySupport<>(mapper, Route.class);
    }

    @Override
    public Optional<Route> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Route> findByCode(String routeCode) {
        return support().findOne(new LambdaQueryWrapper<Route>().eq(Route::getRouteCode, routeCode));
    }

    @Override
    public PageDTO<Route> page(int page, int pageSize, String routeCode) {
        LambdaQueryWrapper<Route> wrapper = support().wrapper();
        if (routeCode != null && !routeCode.isBlank()) {
            wrapper.like(Route::getRouteCode, routeCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Route route) {
        support().save(route);
    }

    @Override
    public void update(Route route) {
        support().update(route);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }

    @Override
    public void lock(Long id) {
        try {
            Route locked = mapper.selectOne(new LambdaQueryWrapper<Route>().eq(Route::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesBaseErrorCodes.ROUTE_NOT_FOUND, "Route 不存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            BusinessException busy = lockConflict(ex);
            if (busy != null) {
                throw busy;
            }
            throw ex;
        }
    }

    /**
     * 翻译 MySQL 行锁错误。3572 是 NOWAIT 立即失败，1205 是等待超时。
     *
     * @param error 数据访问异常
     * @return 业务异常，不是锁冲突时为空
     */
    private static BusinessException lockConflict(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql) {
                if (sql.getErrorCode() == 3572) {
                    return new BusinessException(400102, "对象被占用");
                }
                if (sql.getErrorCode() == 1205) {
                    return new BusinessException(400103, "锁等待超时");
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
