package com.scaffold.mes.exec.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.exec.domain.entity.LotRouteChange;
import com.scaffold.mes.exec.domain.repository.LotRouteChangeRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotRouteChangeMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotRouteChangeDbRepositoryImpl 工艺变更仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotRouteChangeDbRepositoryImpl implements LotRouteChangeRepository {

    private final LotRouteChangeMapper mapper;

    @Override
    public void save(LotRouteChange change) {
        new AppendOnlyRepositorySupport<>(mapper, LotRouteChange.class).save(change);
    }
}
