package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Equipment;

/**
 * EquipmentRepository 设备台账仓储
 * <p>
 * 按当前租户读写 mes_equipment。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 设备，不存在时为空
     */
    Optional<Equipment> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param equipmentCode 设备编码
     * @return 设备，不存在时为空
     */
    Optional<Equipment> findByCode(String equipmentCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param equipmentCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Equipment> page(int page, int pageSize, String equipmentCode);

    /**
     * 插入。
     *
     * @param equipment 设备
     */
    void save(Equipment equipment);

    /**
     * 更新。带乐观锁。
     *
     * @param equipment 已加载的设备
     */
    void update(Equipment equipment);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 按 E10 状态计数。
     *
     * @param e10State E10 状态
     * @return 台数
     */
    long countByE10State(String e10State);

    /**
     * 统计已经切换过的状态历史。
     * <p>
     * 新建时未结束、且没有变更前状态的那一段不算。后续里程碑新增 equipment_id 后要补进这条统计。
     *
     * @param id 设备主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
