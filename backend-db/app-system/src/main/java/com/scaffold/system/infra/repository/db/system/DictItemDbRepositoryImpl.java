package com.scaffold.system.infra.repository.db.system;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.system.DictItem;
import com.scaffold.system.domain.query.system.DictItemQuery;
import com.scaffold.system.domain.repository.system.DictItemRepository;
import com.scaffold.system.infra.mapper.db.system.DictItemMapper;

import lombok.RequiredArgsConstructor;

/**
 * DictItemDbRepositoryImpl 数据库仓储实现
 * <p>
 * 负责字典选项在 DB 热存储模式下的数据访问。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Repository
@RequiredArgsConstructor
public class DictItemDbRepositoryImpl implements DictItemRepository {

    private final DictItemMapper mapper;

    private DbRepositorySupport<DictItem> support() {
        return new DbRepositorySupport<>(mapper, DictItem.class);
    }

    @Override
    public Optional<DictItem> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<DictItem> findByDictTypeAndItemCode(String dictType, String itemCode) {
        if (support().isBlank(dictType) || support().isBlank(itemCode)) {
            return Optional.empty();
        }
        return support().findOne(support().wrapper()
                .eq(DictItem::getDictType, dictType)
                .eq(DictItem::getItemCode, itemCode));
    }

    @Override
    public List<DictItem> findByDictType(String dictType) {
        if (support().isBlank(dictType)) {
            return List.of();
        }
        return support().queryAll(support().wrapper().eq(DictItem::getDictType, dictType));
    }

    @Override
    public List<DictItem> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(DictItem::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public void save(DictItem entity) {
        support().save(entity);
    }

    @Override
    public void update(DictItem entity) {
        support().update(entity);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public PageDTO<DictItem> queryPage(int page, int pageSize, DictItemQuery query) {
        LambdaQueryWrapper<DictItem> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(support().hasText(query.getDictType()), DictItem::getDictType, query.getDictType())
                    .eq(support().hasText(query.getItemCode()), DictItem::getItemCode, query.getItemCode())
                    .like(support().hasText(query.getItemLabel()), DictItem::getItemLabel, query.getItemLabel())
                    .eq(query.getEnabled() != null, DictItem::getEnabled, query.getEnabled());
        }
        return support().queryPage(page, pageSize, wrapper);
    }
}
