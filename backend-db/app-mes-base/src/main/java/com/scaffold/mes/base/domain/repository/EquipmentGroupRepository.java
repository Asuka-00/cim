package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;

/**
 * EquipmentGroupRepository 设备组仓储
 * <p>
 * 按当前租户读写 mes_equipment_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentGroupRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 设备组，不存在时为空
     */
    Optional<EquipmentGroup> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param groupCode 设备组编码
     * @return 设备组，不存在时为空
     */
    Optional<EquipmentGroup> findByCode(String groupCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param groupCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<EquipmentGroup> page(int page, int pageSize, String groupCode);

    /**
     * 插入。
     *
     * @param group 设备组
     */
    void save(EquipmentGroup group);

    /**
     * 更新。
     *
     * @param group 已加载的设备组
     */
    void update(EquipmentGroup group);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计设备和站点绑定的引用数。
     *
     * @param id 设备组主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
