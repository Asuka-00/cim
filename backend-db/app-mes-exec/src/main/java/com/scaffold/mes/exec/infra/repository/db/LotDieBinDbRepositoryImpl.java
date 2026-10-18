package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.repository.LotDieBinRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotDieBinMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotDieBinDbRepositoryImpl Die Bin 仓储实现
 * <p>
 * 汇总行以 wafer_id=0 区分。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotDieBinDbRepositoryImpl implements LotDieBinRepository {

    private final LotDieBinMapper mapper;

    @Override
    public List<LotDieBin> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotDieBin>().eq(LotDieBin::getLotId, lotId));
    }

    @Override
    public void deleteSummary(Long lotId) {
        mapper.delete(new LambdaQueryWrapper<LotDieBin>()
                .eq(LotDieBin::getLotId, lotId)
                .eq(LotDieBin::getWaferId, 0L));
    }

    @Override
    public void deleteByWafer(Long waferId) {
        mapper.delete(new LambdaQueryWrapper<LotDieBin>().eq(LotDieBin::getWaferId, waferId));
    }

    @Override
    public void save(LotDieBin bin) {
        new DbRepositorySupport<>(mapper, LotDieBin.class).save(bin);
    }
}
