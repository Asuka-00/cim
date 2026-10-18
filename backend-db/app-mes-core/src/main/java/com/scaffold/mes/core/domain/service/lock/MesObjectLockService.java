package com.scaffold.mes.core.domain.service.lock;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.lock.MesObjectLock;
import com.scaffold.mes.core.domain.repository.lock.MesObjectLockRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * MesObjectLockService 跨请求对象锁
 * <p>
 * 每次加锁、解锁都是独立短事务,不能套在业务事务里面。过期锁视为已释放。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MesObjectLockService {

    private final MesObjectLockRepository lockRepository;

    /**
     * 占用对象。同一持锁人重复调用会延长过期时间。
     *
     * @param objectType 对象类型
     * @param objectId 对象主键
     * @param owner 持锁人
     * @param purpose 用途,可空
     * @param ttl 从现在起的有效时长
     * @throws BusinessException 对象正被其他人占用
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void acquire(String objectType, Long objectId, String owner, String purpose, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        MesObjectLock current = lockRepository.find(objectType, objectId).orElse(null);
        if (current != null && current.getExpireAt() != null && current.getExpireAt().isAfter(now)) {
            if (owner.equals(current.getLockOwner())) {
                current.setExpireAt(now.plus(ttl));
                current.setPurpose(purpose);
                current.setLockedAt(now);
                lockRepository.update(current);
                return;
            }
            throw new BusinessException(MesErrorCodes.OBJECT_BUSY, "对象被占用");
        }
        if (current != null) {
            lockRepository.deleteById(current.getId());
        }
        MesObjectLock created = new MesObjectLock();
        created.setObjectType(objectType);
        created.setObjectId(objectId);
        created.setLockOwner(owner);
        created.setPurpose(purpose);
        created.setLockedAt(now);
        created.setExpireAt(now.plus(ttl));
        try {
            lockRepository.save(created);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(MesErrorCodes.OBJECT_BUSY, "对象被占用");
        }
    }

    /**
     * 释放对象。锁不存在时直接返回;不是持锁人时拒绝。
     *
     * @param objectType 对象类型
     * @param objectId 对象主键
     * @param owner 持锁人
     * @throws BusinessException 锁属于其他人
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String objectType, Long objectId, String owner) {
        MesObjectLock current = lockRepository.find(objectType, objectId).orElse(null);
        if (current == null) {
            return;
        }
        if (!owner.equals(current.getLockOwner())) {
            throw new BusinessException(MesErrorCodes.OBJECT_BUSY, "对象被占用");
        }
        lockRepository.deleteById(current.getId());
    }
}
