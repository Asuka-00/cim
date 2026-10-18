package com.scaffold.mes.core.infra.mapper.db.lock;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.lock.MesObjectLock;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesObjectLockMapper 对象锁 Mapper
 * <p>
 * 映射 mes_object_lock。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesObjectLockMapper extends BatchBaseMapper<MesObjectLock> {
}
