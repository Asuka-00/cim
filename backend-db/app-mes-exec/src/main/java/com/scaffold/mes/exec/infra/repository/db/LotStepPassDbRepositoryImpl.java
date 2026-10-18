package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotStepPassMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotStepPassDbRepositoryImpl 站点经过次数仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_lot_step_pass。该表没有乐观锁列。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotStepPassDbRepositoryImpl implements LotStepPassRepository {

    private final LotStepPassMapper mapper;

    private DbRepositorySupport<LotStepPass> support() {
        return new DbRepositorySupport<>(mapper, LotStepPass.class);
    }

    @Override
    public List<LotStepPass> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotStepPass>().eq(LotStepPass::getLotId, lotId));
    }

    @Override
    public Optional<LotStepPass> find(Long lotId, Long stepId) {
        return support().findOne(new LambdaQueryWrapper<LotStepPass>()
                .eq(LotStepPass::getLotId, lotId)
                .eq(LotStepPass::getStepId, stepId));
    }

    @Override
    public void save(LotStepPass pass) {
        support().save(pass);
    }

    @Override
    public void update(LotStepPass pass) {
        support().update(pass);
    }
}
