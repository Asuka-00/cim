package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotRelationMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotRelationDbRepositoryImpl Lot 关系仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotRelationDbRepositoryImpl implements LotRelationRepository {

    private final LotRelationMapper mapper;

    @Override
    public void save(LotRelation relation) {
        new AppendOnlyRepositorySupport<>(mapper, LotRelation.class).save(relation);
    }

    @Override
    public boolean existsPackageInput(Long parentLotId) {
        return mapper.selectCount(new LambdaQueryWrapper<LotRelation>()
                .eq(LotRelation::getParentLotId, parentLotId)
                .eq(LotRelation::getRelationType, "PACKAGE_INPUT")) > 0;
    }

    @Override
    public List<LotRelation> listSplits(Long parentLotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotRelation>()
                .eq(LotRelation::getParentLotId, parentLotId)
                .eq(LotRelation::getRelationType, "SPLIT")
                .orderByAsc(LotRelation::getOccurredAt)
                .orderByAsc(LotRelation::getId));
    }

    @Override
    public Optional<LotRelation> findSplitByChild(Long childLotId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LotRelation>()
                .eq(LotRelation::getChildLotId, childLotId)
                .eq(LotRelation::getRelationType, "SPLIT")
                .orderByAsc(LotRelation::getOccurredAt)
                .orderByAsc(LotRelation::getId)
                .last("LIMIT 1")));
    }

    @Override
    public List<LotRelation> listMergesInto(Long targetLotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotRelation>()
                .eq(LotRelation::getChildLotId, targetLotId)
                .eq(LotRelation::getRelationType, "MERGE")
                .orderByAsc(LotRelation::getOccurredAt)
                .orderByAsc(LotRelation::getId));
    }
}
