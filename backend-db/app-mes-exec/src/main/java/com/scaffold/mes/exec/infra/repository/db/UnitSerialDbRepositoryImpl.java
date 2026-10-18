package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.UnitSerial;
import com.scaffold.mes.exec.domain.repository.UnitSerialRepository;
import com.scaffold.mes.exec.infra.mapper.db.UnitSerialMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UnitSerialDbRepositoryImpl Unit 序列号仓储实现
 * <p>
 * 按序列号查重后插入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UnitSerialDbRepositoryImpl implements UnitSerialRepository {

    private final UnitSerialMapper mapper;

    private DbRepositorySupport<UnitSerial> support() {
        return new DbRepositorySupport<>(mapper, UnitSerial.class);
    }

    @Override
    public Optional<UnitSerial> findByNo(String serialNo) {
        return support().findOne(new LambdaQueryWrapper<UnitSerial>().eq(UnitSerial::getSerialNo, serialNo));
    }

    @Override
    public List<UnitSerial> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<UnitSerial>().eq(UnitSerial::getLotId, lotId).orderByAsc(UnitSerial::getSerialNo));
    }

    @Override
    public void save(UnitSerial serial) {
        support().save(serial);
    }
}
