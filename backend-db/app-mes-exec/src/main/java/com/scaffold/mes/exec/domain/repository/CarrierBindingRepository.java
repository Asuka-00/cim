package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.CarrierBinding;

/**
 * CarrierBindingRepository 载具绑定历史仓储
 * <p>
 * 解绑只回填结束时间。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CarrierBindingRepository {

    /**
     * 取某载具仍未结束的绑定。
     *
     * @param carrierId 载具
     * @return 未解绑的记录，没有时为空
     */
    Optional<CarrierBinding> findOpen(Long carrierId);

    /**
     * 插入。
     *
     * @param binding 绑定
     */
    void save(CarrierBinding binding);

    /**
     * 更新结束时间。
     *
     * @param binding 已加载的绑定
     */
    void update(CarrierBinding binding);
}
