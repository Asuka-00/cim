package com.scaffold.mes.base.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.EquipmentCapability;

/**
 * EquipmentCapabilityRepository 设备能力仓储
 * <p>
 * 按当前租户读写 mes_equipment_capability。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentCapabilityRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 能力，不存在时为空
     */
    Optional<EquipmentCapability> findById(Long id);

    /**
     * 按设备、站点、Recipe、产品精确查询。
     *
     * @param equipmentId 设备
     * @param stepId 站点
     * @param recipeId Recipe，0 表示通用
     * @param productId 产品，0 表示通用
     * @return 能力，不存在时为空
     */
    Optional<EquipmentCapability> findExact(Long equipmentId, Long stepId, Long recipeId, Long productId);

    /**
     * 列出某设备在某站点的全部能力。
     *
     * @param equipmentId 设备
     * @param stepId 站点
     * @return 能力列表
     */
    List<EquipmentCapability> listByEquipmentAndStep(Long equipmentId, Long stepId);

    /**
     * 列出某台设备的全部能力。
     *
     * @param equipmentId 设备
     * @return 能力列表
     */
    List<EquipmentCapability> listByEquipment(Long equipmentId);

    /**
     * 列出仍为 VALID 且有效期已过的能力。
     *
     * @param now 当前时间
     * @return 待过期的能力
     */
    List<EquipmentCapability> listDue(LocalDateTime now);

    /**
     * 插入。
     *
     * @param capability 能力
     */
    void save(EquipmentCapability capability);

    /**
     * 更新。
     *
     * @param capability 已加载的能力
     */
    void update(EquipmentCapability capability);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
