package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.WorkOrder;

/**
 * WorkOrderRepository 工单仓储
 * <p>
 * 按当前租户读写 mes_work_order。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WorkOrderRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 工单，不存在时为空
     */
    Optional<WorkOrder> findById(Long id);

    /**
     * 按工单号查询。
     *
     * @param woNo 工单号
     * @return 工单，不存在时为空
     */
    Optional<WorkOrder> findByNo(String woNo);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param woNo 工单号模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<WorkOrder> page(int page, int pageSize, String woNo);

    /**
     * 插入。
     *
     * @param workOrder 工单
     */
    void save(WorkOrder workOrder);

    /**
     * 更新。
     *
     * @param workOrder 已加载的工单
     */
    void update(WorkOrder workOrder);

    /**
     * 锁住工单行，供开批串行化。本方法在开批时使用。
     *
     * @param id 工单主键
     */
    void lock(Long id);
}
