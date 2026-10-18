package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.mes.base.domain.repository.StepCollectItemRepository;
import com.scaffold.mes.base.infra.mapper.db.StepCollectItemMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepCollectItemDbRepositoryImpl 站点采集规格仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepCollectItemDbRepositoryImpl implements StepCollectItemRepository {

    private final StepCollectItemMapper mapper;

    @Override
    public Optional<StepCollectItem> find(Long stepId, Long itemId, Long productId) {
        return new DbRepositorySupport<>(mapper, StepCollectItem.class)
                .findOne(new LambdaQueryWrapper<StepCollectItem>()
                        .eq(StepCollectItem::getStepId, stepId)
                        .eq(StepCollectItem::getItemId, itemId)
                        .eq(StepCollectItem::getProductId, productId));
    }

    @Override
    public void save(StepCollectItem item) {
        new DbRepositorySupport<>(mapper, StepCollectItem.class).save(item);
    }

    @Override
    public List<StepCollectItem> listByStep(Long stepId) {
        return mapper.selectList(new LambdaQueryWrapper<StepCollectItem>().eq(StepCollectItem::getStepId, stepId));
    }
}
