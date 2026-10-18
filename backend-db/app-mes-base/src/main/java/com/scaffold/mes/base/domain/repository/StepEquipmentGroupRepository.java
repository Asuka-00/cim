package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;

/**
 * StepEquipmentGroupRepository 站点设备组绑定仓储
 * <p>
 * 按当前租户读写 mes_step_equipment_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepEquipmentGroupRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 绑定，不存在时为空
     */
    Optional<StepEquipmentGroup> findById(Long id);

    /**
     * 按站点和设备组查询。
     *
     * @param stepId 站点
     * @param groupId 设备组
     * @return 绑定，不存在时为空
     */
    Optional<StepEquipmentGroup> findExact(Long stepId, Long groupId);

    /**
     * 列出某个站点的绑定，优先级从小到大。
     *
     * @param stepId 站点
     * @return 绑定列表
     */
    List<StepEquipmentGroup> listByStep(Long stepId);

    /**
     * 列出某个设备组绑定的站点。
     *
     * @param groupId 设备组
     * @return 绑定列表
     */
    List<StepEquipmentGroup> listByGroup(Long groupId);

    /**
     * 插入。
     *
     * @param binding 绑定
     */
    void save(StepEquipmentGroup binding);

    /**
     * 更新。
     *
     * @param binding 已加载的绑定
     */
    void update(StepEquipmentGroup binding);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
