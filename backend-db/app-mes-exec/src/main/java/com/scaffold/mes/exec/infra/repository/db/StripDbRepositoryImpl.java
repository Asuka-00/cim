package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.Strip;
import com.scaffold.mes.exec.domain.repository.StripRepository;
import com.scaffold.mes.exec.infra.mapper.db.StripMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StripDbRepositoryImpl Strip 仓储实现
 * <p>
 * 按编号查重后插入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StripDbRepositoryImpl implements StripRepository {

    private final StripMapper mapper;

    private DbRepositorySupport<Strip> support() {
        return new DbRepositorySupport<>(mapper, Strip.class);
    }

    @Override
    public Optional<Strip> findByCode(String stripCode) {
        return support().findOne(new LambdaQueryWrapper<Strip>().eq(Strip::getStripCode, stripCode));
    }

    @Override
    public void save(Strip strip) {
        support().save(strip);
    }

    @Override
    public List<Strip> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<Strip>().eq(Strip::getLotId, lotId));
    }

    @Override
    public void update(Strip strip) {
        support().update(strip);
    }
}
