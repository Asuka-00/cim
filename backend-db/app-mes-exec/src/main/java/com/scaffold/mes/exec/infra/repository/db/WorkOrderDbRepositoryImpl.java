package com.scaffold.mes.exec.infra.repository.db;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.mes.exec.infra.mapper.db.WorkOrderMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WorkOrderDbRepositoryImpl 工单仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_work_order。开批前用行锁串行化余量校验。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WorkOrderDbRepositoryImpl implements WorkOrderRepository {

    private final WorkOrderMapper mapper;

    private DbRepositorySupport<WorkOrder> support() {
        return new DbRepositorySupport<>(mapper, WorkOrder.class);
    }

    @Override
    public Optional<WorkOrder> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<WorkOrder> findByNo(String woNo) {
        return support().findOne(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getWoNo, woNo));
    }

    @Override
    public PageDTO<WorkOrder> page(int page, int pageSize, String woNo) {
        LambdaQueryWrapper<WorkOrder> wrapper = support().wrapper();
        if (woNo != null && !woNo.isBlank()) {
            wrapper.like(WorkOrder::getWoNo, woNo);
        }
        return support().queryPage(page, pageSize, wrapper.orderByDesc(WorkOrder::getId));
    }

    @Override
    public void save(WorkOrder workOrder) {
        support().save(workOrder);
    }

    @Override
    public void update(WorkOrder workOrder) {
        support().update(workOrder);
    }

    @Override
    public void lock(Long id) {
        try {
            WorkOrder locked = mapper.selectOne(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在");
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
