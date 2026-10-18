package com.scaffold.mes.core.infra.repository.db.uda;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.repository.uda.UdaDefRepository;
import com.scaffold.mes.core.infra.mapper.db.uda.UdaDefMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UdaDefDbRepositoryImpl 自定义属性定义仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UdaDefDbRepositoryImpl implements UdaDefRepository {

    private final UdaDefMapper mapper;

    @Override
    public List<UdaDef> listByObject(String objectType) {
        return new DbRepositorySupport<>(mapper, UdaDef.class).queryAll(new LambdaQueryWrapper<UdaDef>()
                .eq(UdaDef::getObjectType, objectType)
                .orderByAsc(UdaDef::getAttrCode));
    }

    @Override
    public List<UdaDef> listRequired(String objectType) {
        return new DbRepositorySupport<>(mapper, UdaDef.class).queryAll(new LambdaQueryWrapper<UdaDef>()
                .eq(UdaDef::getObjectType, objectType)
                .eq(UdaDef::getRequired, 1)
                .eq(UdaDef::getEnabled, 1));
    }

    @Override
    public Optional<UdaDef> findByObjectAndCode(String objectType, String attrCode) {
        return new DbRepositorySupport<>(mapper, UdaDef.class).findOne(new LambdaQueryWrapper<UdaDef>()
                .eq(UdaDef::getObjectType, objectType)
                .eq(UdaDef::getAttrCode, attrCode));
    }

    @Override
    public void save(UdaDef def) {
        new DbRepositorySupport<>(mapper, UdaDef.class).save(def);
    }
}
