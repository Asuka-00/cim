package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.infra.mapper.db.BomItemMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * BomItemDbRepositoryImpl BOM 行仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_bom_item。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class BomItemDbRepositoryImpl implements BomItemRepository {

    private final BomItemMapper mapper;

    private DbRepositorySupport<BomItem> support() {
        return new DbRepositorySupport<>(mapper, BomItem.class);
    }

    @Override
    public Optional<BomItem> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<BomItem> findByLine(Long bomVersionId, Integer lineNo) {
        return support().findOne(new LambdaQueryWrapper<BomItem>()
                .eq(BomItem::getBomVersionId, bomVersionId)
                .eq(BomItem::getLineNo, lineNo));
    }

    @Override
    public List<BomItem> listByVersion(Long bomVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<BomItem>()
                .eq(BomItem::getBomVersionId, bomVersionId)
                .orderByAsc(BomItem::getLineNo));
    }

    @Override
    public void save(BomItem item) {
        support().save(item);
    }

    @Override
    public void update(BomItem item) {
        support().update(item);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
