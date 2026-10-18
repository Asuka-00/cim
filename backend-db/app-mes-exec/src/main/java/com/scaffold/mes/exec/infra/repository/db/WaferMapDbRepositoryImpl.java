package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.WaferMap;
import com.scaffold.mes.exec.domain.repository.WaferMapRepository;
import com.scaffold.mes.exec.infra.mapper.db.WaferMapMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WaferMapDbRepositoryImpl Wafer Map 仓储实现
 * <p>
 * 重测时更新旧版本的当前标记。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WaferMapDbRepositoryImpl implements WaferMapRepository {

    private final WaferMapMapper mapper;

    private DbRepositorySupport<WaferMap> support() {
        return new DbRepositorySupport<>(mapper, WaferMap.class);
    }

    @Override
    public List<WaferMap> listByWafer(Long waferId) {
        return mapper.selectList(new LambdaQueryWrapper<WaferMap>().eq(WaferMap::getWaferId, waferId));
    }

    @Override
    public List<WaferMap> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<WaferMap>().eq(WaferMap::getLotId, lotId)
                .orderByAsc(WaferMap::getWaferId)
                .orderByAsc(WaferMap::getMapVersion));
    }

    @Override
    public void save(WaferMap map) {
        support().save(map);
    }

    @Override
    public void update(WaferMap map) {
        support().update(map);
    }
}
