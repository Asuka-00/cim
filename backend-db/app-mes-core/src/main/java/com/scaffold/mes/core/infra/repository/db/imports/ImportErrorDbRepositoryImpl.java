package com.scaffold.mes.core.infra.repository.db.imports;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.imports.ImportError;
import com.scaffold.mes.core.domain.repository.imports.ImportErrorRepository;
import com.scaffold.mes.core.infra.mapper.db.imports.ImportErrorMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ImportErrorDbRepositoryImpl 导入错误明细仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ImportErrorDbRepositoryImpl implements ImportErrorRepository {

    private final ImportErrorMapper mapper;

    @Override
    public void save(ImportError error) {
        new AppendOnlyRepositorySupport<>(mapper, ImportError.class).save(error);
    }

    @Override
    public java.util.List<ImportError> listByTask(Long taskId) {
        return mapper.selectList(new LambdaQueryWrapper<ImportError>()
                .eq(ImportError::getTaskId, taskId)
                .orderByAsc(ImportError::getRowNo));
    }

    @Override
    public boolean exists(Long taskId, Integer rowNo) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ImportError>()
                .eq(ImportError::getTaskId, taskId)
                .eq(ImportError::getRowNo, rowNo));
        return count != null && count > 0;
    }

    @Override
    public Optional<ImportError> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public void deleteByTask(Long taskId) {
        mapper.delete(new LambdaQueryWrapper<ImportError>().eq(ImportError::getTaskId, taskId));
    }
}
