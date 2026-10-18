package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Uom;

/**
 * UomRepository 物料计量单位仓储
 * <p>
 * 按当前租户读写 mes_uom。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UomRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 计量单位，不存在时为空
     */
    Optional<Uom> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param uomCode 单位编码
     * @return 计量单位，不存在时为空
     */
    Optional<Uom> findByCode(String uomCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param uomCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Uom> page(int page, int pageSize, String uomCode);

    /**
     * 插入。
     *
     * @param uom 计量单位
     */
    void save(Uom uom);

    /**
     * 更新。
     *
     * @param uom 已加载的计量单位
     */
    void update(Uom uom);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计换算与物料引用数。
     * <p>
     * 只统计 M1 已建表。后续里程碑新增 uom_id 列时要补进这条统计。
     *
     * @param id 计量单位主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
