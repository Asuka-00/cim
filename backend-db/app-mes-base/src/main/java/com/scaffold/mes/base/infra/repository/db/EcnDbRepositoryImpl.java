package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.Ecn;
import com.scaffold.mes.base.domain.repository.EcnRepository;
import com.scaffold.mes.base.infra.mapper.db.EcnMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EcnDbRepositoryImpl 变更申请仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EcnDbRepositoryImpl implements EcnRepository {

    private final EcnMapper mapper;

    @Override
    public Optional<Ecn> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<Ecn> findByNo(String ecnNo) {
        return new DbRepositorySupport<>(mapper, Ecn.class)
                .findOne(new LambdaQueryWrapper<Ecn>().eq(Ecn::getEcnNo, ecnNo));
    }

    @Override
    public List<Ecn> list() {
        return mapper.selectList(new LambdaQueryWrapper<Ecn>().orderByAsc(Ecn::getEcnNo));
    }

    @Override
    public void save(Ecn ecn) {
        new DbRepositorySupport<>(mapper, Ecn.class).save(ecn);
    }

    @Override
    public void update(Ecn ecn) {
        new DbRepositorySupport<>(mapper, Ecn.class).update(ecn);
    }
}
