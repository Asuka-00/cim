package com.scaffold.mes.core.domain.repository.event;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;

/**
 * MesDomainEventDeliveryRepository 事件投递仓储
 * <p>
 * 按订阅者记录处理状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesDomainEventDeliveryRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 记录,不存在时为空
     */
    Optional<MesDomainEventDelivery> findById(Long id);

    /**
     * 拉取已到重试时间的待处理投递,按主键升序。
     *
     * @param limit 最大条数
     * @return 投递列表
     */
    List<MesDomainEventDelivery> listDue(int limit);

    /**
     * 判断同一订阅者、同一聚合是否还有更早的待处理投递。
     *
     * @param delivery 当前投递
     * @return 存在更早投递时为 true
     */
    boolean hasEarlierPending(MesDomainEventDelivery delivery);

    /**
     * 分页查询投递。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param subscriber 订阅者编码,空表示不限
     * @param status 状态,空表示不限
     * @return 分页结果
     */
    PageDTO<MesDomainEventDelivery> page(int page, int pageSize, String subscriber, String status);

    /**
     * 插入记录。
     *
     * @param delivery 待插入对象
     */
    void save(MesDomainEventDelivery delivery);

    /**
     * 更新记录。
     *
     * @param delivery 已加载的对象
     */
    void update(MesDomainEventDelivery delivery);
}
