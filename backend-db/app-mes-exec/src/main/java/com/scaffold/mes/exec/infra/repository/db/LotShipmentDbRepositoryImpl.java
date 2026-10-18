package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotShipment;
import com.scaffold.mes.exec.domain.repository.LotShipmentRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotShipmentMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotShipmentDbRepositoryImpl 出货记录仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_lot_shipment。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotShipmentDbRepositoryImpl implements LotShipmentRepository {

    private final LotShipmentMapper mapper;

    private DbRepositorySupport<LotShipment> support() {
        return new DbRepositorySupport<>(mapper, LotShipment.class);
    }

    @Override
    public Optional<LotShipment> findByNo(String shipNo) {
        return support().findOne(new LambdaQueryWrapper<LotShipment>().eq(LotShipment::getShipNo, shipNo));
    }

    @Override
    public void save(LotShipment shipment) {
        support().save(shipment);
    }
}
