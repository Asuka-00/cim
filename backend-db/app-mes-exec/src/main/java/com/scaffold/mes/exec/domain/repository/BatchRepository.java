package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.Batch;

/**
 * BatchRepository 批次仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BatchRepository {

    /**
     * 按主键查询。
     *
     * @param id 批次
     * @return 批次，不存在时为空
     */
    Optional<Batch> findById(Long id);

    /**
     * 按批次号查询。
     *
     * @param batchNo 批次号
     * @return 批次，不存在时为空
     */
    Optional<Batch> findByNo(String batchNo);

    /**
     * 插入。
     *
     * @param batch 批次
     */
    void save(Batch batch);

    /**
     * 更新。
     *
     * @param batch 已加载的批次
     */
    void update(Batch batch);

    /**
     * 列出批次。
     *
     * @return 按批次号排列，没有时为空列表
     */
    List<Batch> list();
}
