package com.scaffold.mes.exec.infra.repository.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.infra.mapper.db.WaferMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WaferDbRepositoryImpl Wafer 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_wafer。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WaferDbRepositoryImpl implements WaferRepository {

    private final WaferMapper mapper;

    private DbRepositorySupport<Wafer> support() {
        return new DbRepositorySupport<>(mapper, Wafer.class);
    }

    @Override
    public PageDTO<Wafer> page(int page, int pageSize, String waferCode, Long lotId) {
        LambdaQueryWrapper<Wafer> wrapper = support().wrapper();
        if (waferCode != null && !waferCode.isBlank()) {
            wrapper.like(Wafer::getWaferCode, waferCode);
        }
        if (lotId != null) {
            wrapper.eq(Wafer::getLotId, lotId);
        }
        return support().queryPage(page, pageSize, wrapper.orderByDesc(Wafer::getId));
    }

    @Override
    public Optional<Wafer> findByCode(String waferCode) {
        return support().findOne(new LambdaQueryWrapper<Wafer>().eq(Wafer::getWaferCode, waferCode));
    }

    @Override
    public Optional<Wafer> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<Wafer> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<Wafer>().eq(Wafer::getLotId, lotId));
    }

    @Override
    public void save(Wafer wafer) {
        support().save(wafer);
    }

    @Override
    public void update(Wafer wafer) {
        support().update(wafer);
    }

    @Override
    public void lock(Long id) {
        try {
            Wafer locked = mapper.selectOne(new LambdaQueryWrapper<Wafer>().eq(Wafer::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在");
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
}
