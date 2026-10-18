package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.Carrier;

/**
 * CarrierRepository 载具仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CarrierRepository {

    /**
     * 按主键查询。
     *
     * @param id 载具
     * @return 载具，不存在时为空
     */
    Optional<Carrier> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param carrierCode 编码
     * @return 载具，不存在时为空
     */
    Optional<Carrier> findByCode(String carrierCode);

    /**
     * 查当前绑着某 Lot 的载具。
     *
     * @param lotId Lot
     * @return 载具，没有时为空
     */
    Optional<Carrier> findByCurrentLot(Long lotId);

    /**
     * 列出全部载具，按编码。
     *
     * @return 载具
     */
    List<Carrier> list();

    /**
     * 插入。
     *
     * @param carrier 载具
     */
    void save(Carrier carrier);

    /**
     * 更新。
     *
     * @param carrier 已加载的载具
     */
    void update(Carrier carrier);
}
