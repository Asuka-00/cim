package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Step;

/**
 * StepRepository 站点仓储
 * <p>
 * 按当前租户读写 mes_step。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 站点，不存在时为空
     */
    Optional<Step> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param stepCode 站点编码
     * @return 站点，不存在时为空
     */
    Optional<Step> findByCode(String stepCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param stepCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Step> page(int page, int pageSize, String stepCode);

    /**
     * 插入。
     *
     * @param step 站点
     */
    void save(Step step);

    /**
     * 更新。
     *
     * @param step 已加载的站点
     */
    void update(Step step);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计站点设备组和站点 Recipe 绑定。
     *
     * @param id 站点主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
