package com.scaffold.mes.core.domain.repository.event;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;

/**
 * MesDomainEventRepository 领域事件仓储
 * <p>
 * 写入事件并拉取尚未分发的事件。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesDomainEventRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 记录,不存在时为空
     */
    Optional<MesDomainEvent> findById(Long id);

    /**
     * 按主键升序拉取尚未分发的事件。
     *
     * @param limit 最大条数
     * @return 新事件列表
     */
    List<MesDomainEvent> listNew(int limit);

    /**
     * 分页查询领域事件。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param status 分发状态,空表示不限
     * @return 分页结果
     */
    PageDTO<MesDomainEvent> page(int page, int pageSize, String status);

    /**
     * 插入记录。
     *
     * @param event 待插入对象
     */
    void save(MesDomainEvent event);

    /**
     * 更新记录。
     *
     * @param event 已加载的对象
     */
    void update(MesDomainEvent event);
}
