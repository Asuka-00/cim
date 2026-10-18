package com.scaffold.mes.exec.infra.repository.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotDbRepositoryImpl Lot 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotDbRepositoryImpl implements LotRepository {

    private final LotMapper mapper;

    private DbRepositorySupport<Lot> support() {
        return new DbRepositorySupport<>(mapper, Lot.class);
    }

    @Override
    public PageDTO<Lot> page(int page, int pageSize, String lotNo, String status) {
        LambdaQueryWrapper<Lot> wrapper = support().wrapper();
        if (lotNo != null && !lotNo.isBlank()) {
            wrapper.like(Lot::getLotNo, lotNo);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Lot::getStatus, status);
        }
        return support().queryPage(page, pageSize, wrapper.orderByDesc(Lot::getId));
    }

    @Override
    public Optional<Lot> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Lot> findByNo(String lotNo) {
        return support().findOne(new LambdaQueryWrapper<Lot>().eq(Lot::getLotNo, lotNo));
    }

    @Override
    public long sumInitialQty(Long woId) {
        return mapper.sumInitialQty(woId);
    }

    @Override
    public void save(Lot lot) {
        support().save(lot);
    }

    @Override
    public void update(Lot lot) {
        support().update(lot);
    }

    @Override
    public void lock(Long id) {
        try {
            Lot locked = mapper.selectOne(new LambdaQueryWrapper<Lot>().eq(Lot::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            Throwable current = ex;
            while (current != null) {
                if (current instanceof SQLException sql && sql.getErrorCode() == 3572) {
                    throw new BusinessException(400102, "对象被占用");
                }
                if (current instanceof SQLException sql && sql.getErrorCode() == 1205) {
                    throw new BusinessException(400103, "锁等待超时");
                }
                current = current.getCause();
            }
            throw ex;
        }
    }

    @Override
    public int maxSplitSeq(Long parentLotId) {
        return mapper.maxSplitSeq(parentLotId);
    }

    @Override
    public List<Lot> listWaiting(Long stepId, Long areaId) {
        LambdaQueryWrapper<Lot> wrapper = new LambdaQueryWrapper<Lot>()
                .eq(Lot::getStatus, "WAITING")
                .and(hold -> hold.eq(Lot::getHoldCount, 0).or().isNull(Lot::getHoldCount));
        if (stepId != null) {
            wrapper.eq(Lot::getStepId, stepId);
        }
        if (areaId != null) {
            wrapper.eq(Lot::getLocationType, "AREA").eq(Lot::getLocationId, areaId);
        }
        return mapper.selectList(wrapper);
    }

    @Override
    public List<Lot> listByWorkOrder(Long woId) {
        return mapper.selectList(new LambdaQueryWrapper<Lot>().eq(Lot::getWoId, woId));
    }

    @Override
    public long countRunning(Long equipmentId) {
        return mapper.countRunning(equipmentId);
    }

    @Override
    public long countOccupancy(Long equipmentId, Long excludeBatchId) {
        return mapper.countOccupancy(equipmentId, excludeBatchId);
    }

    @Override
    public long countRunningOnPort(Long portId) {
        return mapper.countRunningOnPort(portId);
    }

    @Override
    public List<Lot> listCompletedBetween(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return mapper.selectList(new LambdaQueryWrapper<Lot>()
                .eq(Lot::getStatus, "COMPLETED")
                .ge(Lot::getFinishedAt, from)
                .lt(Lot::getFinishedAt, to));
    }

    @Override
    public List<Lot> listInProcess() {
        return mapper.selectList(new LambdaQueryWrapper<Lot>().in(Lot::getStatus, java.util.List.of("WAITING", "RUNNING")));
    }
}
