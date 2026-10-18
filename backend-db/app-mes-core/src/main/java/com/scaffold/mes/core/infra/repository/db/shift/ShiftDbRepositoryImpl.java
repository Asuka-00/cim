package com.scaffold.mes.core.infra.repository.db.shift;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.mes.core.domain.repository.shift.ShiftRepository;
import com.scaffold.mes.core.infra.mapper.db.shift.ShiftMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ShiftDbRepositoryImpl 班次仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ShiftDbRepositoryImpl implements ShiftRepository {

    private final ShiftMapper mapper;

    @Override
    public Optional<Shift> findByCode(String shiftCode) {
        return new DbRepositorySupport<>(mapper, Shift.class)
                .findOne(new LambdaQueryWrapper<Shift>().eq(Shift::getShiftCode, shiftCode));
    }

    @Override
    public List<Shift> list() {
        return mapper.selectList(new LambdaQueryWrapper<Shift>().orderByAsc(Shift::getShiftCode));
    }

    @Override
    public void save(Shift shift) {
        new DbRepositorySupport<>(mapper, Shift.class).save(shift);
    }
}
