package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.EquipmentFault;
import com.scaffold.mes.base.domain.entity.EquipmentFaultLot;

/**
 * EquipmentFaultRepository 设备异常仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentFaultRepository {

    /**
     * 按单号查询。
     *
     * @param faultNo 单号
     * @return 异常，不存在时为空
     */
    Optional<EquipmentFault> findByNo(String faultNo);

    /**
     * 按主键查询。
     *
     * @param id 异常
     * @return 异常，不存在时为空
     */
    Optional<EquipmentFault> findById(Long id);

    /**
     * 列出全部异常，按单号。
     *
     * @return 异常
     */
    List<EquipmentFault> list();

    /**
     * 插入异常。
     *
     * @param fault 异常
     */
    void save(EquipmentFault fault);

    /**
     * 更新异常。关闭时写处理结果。
     *
     * @param fault 已加载的异常
     */
    void update(EquipmentFault fault);

    /**
     * 插入受影响的 Lot。
     *
     * @param link 关联
     */
    void saveLot(EquipmentFaultLot link);
}
