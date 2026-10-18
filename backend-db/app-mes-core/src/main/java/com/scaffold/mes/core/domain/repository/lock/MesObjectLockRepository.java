package com.scaffold.mes.core.domain.repository.lock;

import java.util.Optional;

import com.scaffold.mes.core.domain.entity.lock.MesObjectLock;

/**
 * MesObjectLockRepository 对象锁仓储
 * <p>
 * 按当前租户查找一个对象上的锁。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesObjectLockRepository {

    /**
     * 按对象查找锁。
     *
     * @param objectType 对象类型
     * @param objectId 对象主键
     * @return 锁,不存在时为空
     */
    Optional<MesObjectLock> find(String objectType, Long objectId);

    /**
     * 插入锁。
     *
     * @param lock 锁
     */
    void save(MesObjectLock lock);

    /**
     * 更新过期时间。
     *
     * @param lock 已加载的锁
     */
    void update(MesObjectLock lock);

    /**
     * 删除锁。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
