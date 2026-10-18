package com.scaffold.mes.core.infra.repository.db.print;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.print.PrintLog;
import com.scaffold.mes.core.domain.repository.print.PrintLogRepository;
import com.scaffold.mes.core.infra.mapper.db.print.PrintLogMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * PrintLogDbRepositoryImpl 打印记录仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class PrintLogDbRepositoryImpl implements PrintLogRepository {

    private final PrintLogMapper mapper;

    @Override
    public boolean exists(String bizType, Long bizId, Long tplId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<PrintLog>()
                .eq(PrintLog::getBizType, bizType)
                .eq(PrintLog::getBizId, bizId)
                .eq(PrintLog::getTplId, tplId));
        return count != null && count > 0;
    }

    @Override
    public java.util.List<PrintLog> listByBiz(String bizType, Long bizId) {
        return mapper.selectList(new LambdaQueryWrapper<PrintLog>()
                .eq(PrintLog::getBizType, bizType)
                .eq(PrintLog::getBizId, bizId)
                .orderByAsc(PrintLog::getPrintedAt));
    }

    @Override
    public void save(PrintLog log) {
        new AppendOnlyRepositorySupport<>(mapper, PrintLog.class).save(log);
    }
}
