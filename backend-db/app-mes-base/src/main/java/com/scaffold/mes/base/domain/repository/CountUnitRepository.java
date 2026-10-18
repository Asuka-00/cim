package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.CountUnit;

/**
 * CountUnitRepository 计数单位仓储
 * <p>
 * 按当前租户读写 mes_count_unit。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CountUnitRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 计数单位，不存在时为空
     */
    Optional<CountUnit> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param unitCode 单位编码
     * @return 计数单位，不存在时为空
     */
    Optional<CountUnit> findByCode(String unitCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param unitCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<CountUnit> page(int page, int pageSize, String unitCode);

    /**
     * 插入。
     *
     * @param unit 计数单位
     */
    void save(CountUnit unit);

    /**
     * 更新。
     *
     * @param unit 已加载的计数单位
     */
    void update(CountUnit unit);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计产品与换算引用数。
     *
     * @param id 计数单位主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
