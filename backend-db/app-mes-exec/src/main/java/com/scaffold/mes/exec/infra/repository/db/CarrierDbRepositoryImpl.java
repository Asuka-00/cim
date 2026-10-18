package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.Carrier;
import com.scaffold.mes.exec.domain.repository.CarrierRepository;
import com.scaffold.mes.exec.infra.mapper.db.CarrierMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * CarrierDbRepositoryImpl 载具仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class CarrierDbRepositoryImpl implements CarrierRepository {

    private final CarrierMapper mapper;

    private DbRepositorySupport<Carrier> support() {
        return new DbRepositorySupport<>(mapper, Carrier.class);
    }

    @Override
    public Optional<Carrier> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Carrier> findByCode(String carrierCode) {
        return support().findOne(new LambdaQueryWrapper<Carrier>().eq(Carrier::getCarrierCode, carrierCode));
    }

    @Override
    public Optional<Carrier> findByCurrentLot(Long lotId) {
        return support().findOne(new LambdaQueryWrapper<Carrier>().eq(Carrier::getCurrentLotId, lotId));
    }

    @Override
    public List<Carrier> list() {
        return mapper.selectList(new LambdaQueryWrapper<Carrier>().orderByAsc(Carrier::getCarrierCode));
    }

    @Override
    public void save(Carrier carrier) {
        support().save(carrier);
    }

    @Override
    public void update(Carrier carrier) {
        support().update(carrier);
    }
}
