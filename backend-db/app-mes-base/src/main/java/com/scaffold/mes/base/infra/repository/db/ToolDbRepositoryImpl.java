package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.repository.ToolRepository;
import com.scaffold.mes.base.infra.mapper.db.ToolMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ToolDbRepositoryImpl 工具仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ToolDbRepositoryImpl implements ToolRepository {

    private final ToolMapper mapper;

    private DbRepositorySupport<Tool> support() {
        return new DbRepositorySupport<>(mapper, Tool.class);
    }

    @Override
    public Optional<Tool> findByCode(String toolCode) {
        return support().findOne(new LambdaQueryWrapper<Tool>().eq(Tool::getToolCode, toolCode));
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<Tool> list() {
        return mapper.selectList(new LambdaQueryWrapper<Tool>().orderByAsc(Tool::getToolCode));
    }

    @Override
    public void save(Tool tool) {
        support().save(tool);
    }

    @Override
    public void update(Tool tool) {
        support().update(tool);
    }
}
