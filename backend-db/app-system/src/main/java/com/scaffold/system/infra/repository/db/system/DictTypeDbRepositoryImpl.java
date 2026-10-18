package com.scaffold.system.infra.repository.db.system;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.system.DictType;
import com.scaffold.system.domain.query.system.DictTypeQuery;
import com.scaffold.system.domain.repository.system.DictTypeRepository;
import com.scaffold.system.infra.mapper.db.system.DictTypeMapper;

import lombok.RequiredArgsConstructor;

/**
 * DictTypeDbRepositoryImpl 数据库仓储实现
 * <p>
 * 负责字典类型在 DB 热存储模式下的数据访问。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Repository
@RequiredArgsConstructor
public class DictTypeDbRepositoryImpl implements DictTypeRepository {

    private final DictTypeMapper mapper;

    private DbRepositorySupport<DictType> support() {
        return new DbRepositorySupport<>(mapper, DictType.class);
    }

    @Override
    public Optional<DictType> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<DictType> findByDictType(String dictType) {
        if (support().isBlank(dictType)) {
            return Optional.empty();
        }
        return support().findOne(support().wrapper().eq(DictType::getDictType, dictType));
    }

    @Override
    public List<DictType> findAll() {
        return support().findAll();
    }

    @Override
    public List<DictType> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(DictType::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public void save(DictType entity) {
        support().save(entity);
    }

    @Override
    public void update(DictType entity) {
        support().update(entity);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public PageDTO<DictType> queryPage(int page, int pageSize, DictTypeQuery query) {
        LambdaQueryWrapper<DictType> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(support().hasText(query.getDictType()), DictType::getDictType, query.getDictType())
                    .like(support().hasText(query.getDictName()), DictType::getDictName, query.getDictName())
                    .eq(query.getEnabled() != null, DictType::getEnabled, query.getEnabled());
        }
        return support().queryPage(page, pageSize, wrapper);
    }
}
