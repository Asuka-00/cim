package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.infra.mapper.db.BatchMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * BatchDbRepositoryImpl 批次仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class BatchDbRepositoryImpl implements BatchRepository {

    private final BatchMapper mapper;

    private DbRepositorySupport<Batch> support() {
        return new DbRepositorySupport<>(mapper, Batch.class);
    }

    @Override
    public Optional<Batch> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Batch> findByNo(String batchNo) {
        return support().findOne(new LambdaQueryWrapper<Batch>().eq(Batch::getBatchNo, batchNo));
    }

    @Override
    public void save(Batch batch) {
        support().save(batch);
    }

    @Override
    public void update(Batch batch) {
        support().update(batch);
    }

    @Override
    public List<Batch> list() {
        return mapper.selectList(new LambdaQueryWrapper<Batch>().orderByAsc(Batch::getBatchNo));
    }
}
