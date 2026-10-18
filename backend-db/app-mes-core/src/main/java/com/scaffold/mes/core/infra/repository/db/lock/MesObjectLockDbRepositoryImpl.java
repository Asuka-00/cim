package com.scaffold.mes.core.infra.repository.db.lock;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.lock.MesObjectLock;
import com.scaffold.mes.core.domain.repository.lock.MesObjectLockRepository;
import com.scaffold.mes.core.infra.mapper.db.lock.MesObjectLockMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesObjectLockDbRepositoryImpl 对象锁仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_object_lock。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesObjectLockDbRepositoryImpl implements MesObjectLockRepository {

    private final MesObjectLockMapper mapper;

    private DbRepositorySupport<MesObjectLock> support() {
        return new DbRepositorySupport<>(mapper, MesObjectLock.class);
    }

    @Override
    public Optional<MesObjectLock> find(String objectType, Long objectId) {
        return support().findOne(new LambdaQueryWrapper<MesObjectLock>()
                .eq(MesObjectLock::getObjectType, objectType)
                .eq(MesObjectLock::getObjectId, objectId));
    }

    @Override
    public void save(MesObjectLock lock) {
        support().save(lock);
    }

    @Override
    public void update(MesObjectLock lock) {
        support().update(lock);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
