package com.scaffold.mes.base.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.Esign;
import com.scaffold.mes.base.domain.repository.EsignRepository;
import com.scaffold.mes.base.infra.mapper.db.EsignMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EsignDbRepositoryImpl 电子签名仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EsignDbRepositoryImpl implements EsignRepository {

    private final EsignMapper mapper;

    @Override
    public void save(Esign esign) {
        new AppendOnlyRepositorySupport<>(mapper, Esign.class).save(esign);
    }

    @Override
    public boolean exists(String actionCode, String bizType, Long bizId, String signType) {
        return mapper.selectCount(new LambdaQueryWrapper<Esign>()
                .eq(Esign::getActionCode, actionCode)
                .eq(Esign::getBizType, bizType)
                .eq(Esign::getBizId, bizId)
                .eq(Esign::getSignType, signType)) > 0;
    }
}
