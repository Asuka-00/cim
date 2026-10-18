package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.UomConversion;

/**
 * UomConversionRepository 物料单位换算仓储
 * <p>
 * 按当前租户读写 mes_uom_conversion。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UomConversionRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 换算，不存在时为空
     */
    Optional<UomConversion> findById(Long id);

    /**
     * 按源单位、目标单位和物料精确查询。
     *
     * @param fromUomId 源单位
     * @param toUomId 目标单位
     * @param materialId 限定物料，0 表示通用
     * @return 换算，不存在时为空
     */
    Optional<UomConversion> findExact(Long fromUomId, Long toUomId, Long materialId);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param fromUomId 源单位，空表示不限
     * @return 分页结果
     */
    PageDTO<UomConversion> page(int page, int pageSize, Long fromUomId);

    /**
     * 插入。
     *
     * @param conversion 换算
     */
    void save(UomConversion conversion);

    /**
     * 更新。
     *
     * @param conversion 已加载的换算
     */
    void update(UomConversion conversion);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
