package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.StepGroup;

/**
 * StepGroupRepository 站点组仓储
 * <p>
 * 按当前租户读写 mes_step_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepGroupRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 站点组，不存在时为空
     */
    Optional<StepGroup> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param groupCode 站点组编码
     * @return 站点组，不存在时为空
     */
    Optional<StepGroup> findByCode(String groupCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param groupCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<StepGroup> page(int page, int pageSize, String groupCode);

    /**
     * 插入。
     *
     * @param group 站点组
     */
    void save(StepGroup group);

    /**
     * 更新。
     *
     * @param group 已加载的站点组
     */
    void update(StepGroup group);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计站点引用数。
     *
     * @param id 站点组主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
