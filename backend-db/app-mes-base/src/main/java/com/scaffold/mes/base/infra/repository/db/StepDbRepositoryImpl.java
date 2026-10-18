package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.mes.base.infra.mapper.db.StepMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepDbRepositoryImpl 站点仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_step。M1 没有引用站点的表，引用统计固定为 0。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepDbRepositoryImpl implements StepRepository {

    private final StepMapper mapper;

    private DbRepositorySupport<Step> support() {
        return new DbRepositorySupport<>(mapper, Step.class);
    }

    @Override
    public Optional<Step> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Step> findByCode(String stepCode) {
        return support().findOne(new LambdaQueryWrapper<Step>().eq(Step::getStepCode, stepCode));
    }

    @Override
    public PageDTO<Step> page(int page, int pageSize, String stepCode) {
        LambdaQueryWrapper<Step> wrapper = support().wrapper();
        if (stepCode != null && !stepCode.isBlank()) {
            wrapper.like(Step::getStepCode, stepCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Step step) {
        support().save(step);
    }

    @Override
    public void update(Step step) {
        support().update(step);
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
