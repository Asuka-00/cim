package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.PackContainer;

/**
 * PackContainerRepository 包装容器仓储
 * <p>
 * 读写 mes_pack_container。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface PackContainerRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 容器，不存在时为空
     */
    Optional<PackContainer> findById(Long id);

    /**
     * 按容器号查询。
     *
     * @param containerNo 容器号
     * @return 容器，不存在时为空
     */
    Optional<PackContainer> findByNo(String containerNo);

    /**
     * 列出某个 Lot 的容器。
     *
     * @param lotId Lot
     * @return 按容器号排列
     */
    List<PackContainer> listByLot(Long lotId);

    /**
     * 插入。
     *
     * @param container 容器
     */
    void save(PackContainer container);

    /**
     * 更新。
     *
     * @param container 已加载的容器
     */
    void update(PackContainer container);
}
