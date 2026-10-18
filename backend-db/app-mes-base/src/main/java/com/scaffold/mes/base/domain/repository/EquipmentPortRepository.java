package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.EquipmentPort;

/**
 * EquipmentPortRepository 设备端口仓储
 * <p>
 * 按当前租户读写 mes_equipment_port。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentPortRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 端口，不存在时为空
     */
    Optional<EquipmentPort> findById(Long id);

    /**
     * 按设备和编码查询。
     *
     * @param equipmentId 设备
     * @param portCode 端口编码
     * @return 端口，不存在时为空
     */
    Optional<EquipmentPort> findByCode(Long equipmentId, String portCode);

    /**
     * 列出某设备的端口。
     *
     * @param equipmentId 设备
     * @return 端口列表
     */
    List<EquipmentPort> listByEquipment(Long equipmentId);

    /**
     * 插入。
     *
     * @param port 端口
     */
    void save(EquipmentPort port);

    /**
     * 更新。带乐观锁。
     *
     * @param port 已加载的端口
     */
    void update(EquipmentPort port);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
