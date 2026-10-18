package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.CarrierBinding;
import com.scaffold.mes.exec.domain.repository.CarrierBindingRepository;
import com.scaffold.mes.exec.infra.mapper.db.CarrierBindingMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * CarrierBindingDbRepositoryImpl 载具绑定历史仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class CarrierBindingDbRepositoryImpl implements CarrierBindingRepository {

    private final CarrierBindingMapper mapper;

    private DbRepositorySupport<CarrierBinding> support() {
        return new DbRepositorySupport<>(mapper, CarrierBinding.class);
    }

    @Override
    public Optional<CarrierBinding> findOpen(Long carrierId) {
        return support().findOne(new LambdaQueryWrapper<CarrierBinding>()
                .eq(CarrierBinding::getCarrierId, carrierId)
                .isNull(CarrierBinding::getUnboundAt));
    }

    @Override
    public void save(CarrierBinding binding) {
        support().save(binding);
    }

    @Override
    public void update(CarrierBinding binding) {
        support().update(binding);
    }
}
