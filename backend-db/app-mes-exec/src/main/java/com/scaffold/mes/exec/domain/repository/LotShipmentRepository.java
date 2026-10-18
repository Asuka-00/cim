package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotShipment;

/**
 * LotShipmentRepository 出货记录仓储
 * <p>
 * 按当前租户读写 mes_lot_shipment。出货单号唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotShipmentRepository {

    /**
     * 按出货单号查询。
     *
     * @param shipNo 出货单号
     * @return 记录，不存在时为空
     */
    Optional<LotShipment> findByNo(String shipNo);

    /**
     * 插入。
     *
     * @param shipment 出货记录
     */
    void save(LotShipment shipment);
}
