package com.scaffold.mes.exec.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.exec.domain.entity.InterlockLog;
import com.scaffold.mes.exec.domain.repository.InterlockLogRepository;
import com.scaffold.mes.exec.infra.mapper.db.InterlockLogMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * InterlockLogDbRepositoryImpl 进站阻断日志仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class InterlockLogDbRepositoryImpl implements InterlockLogRepository {

    private final InterlockLogMapper mapper;

    @Override
    public void save(InterlockLog log) {
        new AppendOnlyRepositorySupport<>(mapper, InterlockLog.class).save(log);
    }
}
