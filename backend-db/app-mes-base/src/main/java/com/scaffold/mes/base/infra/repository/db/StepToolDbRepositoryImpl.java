package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepTool;
import com.scaffold.mes.base.domain.repository.StepToolRepository;
import com.scaffold.mes.base.infra.mapper.db.StepToolMapper;

import lombok.RequiredArgsConstructor;

/**
 * StepToolDbRepositoryImpl 站点工具要求仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepToolDbRepositoryImpl implements StepToolRepository {

    private final StepToolMapper mapper;

    @Override
    public List<StepTool> listByStep(Long stepId) {
        return mapper.selectList(new LambdaQueryWrapper<StepTool>().eq(StepTool::getStepId, stepId));
    }
}
