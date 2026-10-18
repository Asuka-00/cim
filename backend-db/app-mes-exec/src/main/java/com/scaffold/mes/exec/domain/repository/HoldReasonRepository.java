package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.HoldReason;

/**
 * HoldReasonRepository Hold 原因仓储
 * <p>
 * 按当前租户读写 mes_hold_reason。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface HoldReasonRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 原因，不存在时为空
     */
    Optional<HoldReason> findById(Long id);

    /**
     * 按原因码查询。
     *
     * @param reasonCode 原因码
     * @return 原因，不存在时为空
     */
    Optional<HoldReason> findByCode(String reasonCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param reasonCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<HoldReason> page(int page, int pageSize, String reasonCode);

    /**
     * 插入。
     *
     * @param reason 原因
     */
    void save(HoldReason reason);

    /**
     * 更新。
     *
     * @param reason 已加载的原因
     */
    void update(HoldReason reason);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计仍引用该原因码的 Lot Hold 行数。
     *
     * @param reasonCode 原因码
     * @return 引用条数
     */
    long countHolds(String reasonCode);
}
