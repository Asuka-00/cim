package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.StepGroup;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.mes.base.infra.mapper.db.StepGroupMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepGroupDbRepositoryImpl 站点组仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_step_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepGroupDbRepositoryImpl implements StepGroupRepository {

    private final StepGroupMapper mapper;

    private DbRepositorySupport<StepGroup> support() {
        return new DbRepositorySupport<>(mapper, StepGroup.class);
    }

    @Override
    public Optional<StepGroup> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<StepGroup> findByCode(String groupCode) {
        return support().findOne(new LambdaQueryWrapper<StepGroup>().eq(StepGroup::getGroupCode, groupCode));
    }

    @Override
    public PageDTO<StepGroup> page(int page, int pageSize, String groupCode) {
        LambdaQueryWrapper<StepGroup> wrapper = support().wrapper();
        if (groupCode != null && !groupCode.isBlank()) {
            wrapper.like(StepGroup::getGroupCode, groupCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(StepGroup group) {
        support().save(group);
    }

    @Override
    public void update(StepGroup group) {
        support().update(group);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
