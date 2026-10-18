package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Area;

/**
 * AreaRepository 区域仓储
 * <p>
 * 按当前租户读写 mes_area。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface AreaRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 区域，不存在时为空
     */
    Optional<Area> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param areaCode 区域编码
     * @return 区域，不存在时为空
     */
    Optional<Area> findByCode(String areaCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param areaCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Area> page(int page, int pageSize, String areaCode);

    /**
     * 插入。
     *
     * @param area 区域
     */
    void save(Area area);

    /**
     * 更新。
     *
     * @param area 已加载的区域
     */
    void update(Area area);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计下级区域、站点、设备组和设备的引用数。
     *
     * @param id 区域主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
