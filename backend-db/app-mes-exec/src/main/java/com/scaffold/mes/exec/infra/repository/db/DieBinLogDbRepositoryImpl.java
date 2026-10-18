package com.scaffold.mes.exec.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.exec.domain.entity.DieBinLog;
import com.scaffold.mes.exec.domain.repository.DieBinLogRepository;
import com.scaffold.mes.exec.infra.mapper.db.DieBinLogMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * DieBinLogDbRepositoryImpl Die Bin 流水仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class DieBinLogDbRepositoryImpl implements DieBinLogRepository {

    private final DieBinLogMapper mapper;

    @Override
    public void save(DieBinLog log) {
        new AppendOnlyRepositorySupport<>(mapper, DieBinLog.class).save(log);
    }
}
