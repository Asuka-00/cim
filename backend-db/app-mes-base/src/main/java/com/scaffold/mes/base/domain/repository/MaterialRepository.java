package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Material;

/**
 * MaterialRepository 物料仓储
 * <p>
 * 按当前租户读写 mes_material。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MaterialRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 物料，不存在时为空
     */
    Optional<Material> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param materialCode 物料编码
     * @return 物料，不存在时为空
     */
    Optional<Material> findByCode(String materialCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param materialCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Material> page(int page, int pageSize, String materialCode);

    /**
     * 插入。
     *
     * @param material 物料
     */
    void save(Material material);

    /**
     * 更新。
     *
     * @param material 已加载的物料
     */
    void update(Material material);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计单位换算引用数。
     * <p>
     * 只统计 M1 的 mes_uom_conversion。后续里程碑新增 material_id 后要补进这条统计。
     *
     * @param id 物料主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
