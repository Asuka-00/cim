package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepInstruction;
import com.scaffold.mes.base.domain.repository.StepInstructionRepository;
import com.scaffold.mes.base.infra.mapper.db.StepInstructionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepInstructionDbRepositoryImpl 站点作业指导仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepInstructionDbRepositoryImpl implements StepInstructionRepository {

    private final StepInstructionMapper mapper;

    @Override
    public Optional<StepInstruction> find(Long stepId, Long productId) {
        return new DbRepositorySupport<>(mapper, StepInstruction.class)
                .findOne(new LambdaQueryWrapper<StepInstruction>()
                        .eq(StepInstruction::getStepId, stepId)
                        .eq(StepInstruction::getProductId, productId));
    }

    @Override
    public void save(StepInstruction instruction) {
        new DbRepositorySupport<>(mapper, StepInstruction.class).save(instruction);
    }
}
