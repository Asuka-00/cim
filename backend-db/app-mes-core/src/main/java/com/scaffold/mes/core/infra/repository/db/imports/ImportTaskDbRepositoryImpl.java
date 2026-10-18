package com.scaffold.mes.core.infra.repository.db.imports;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.mes.core.infra.mapper.db.imports.ImportTaskMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ImportTaskDbRepositoryImpl 导入任务仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ImportTaskDbRepositoryImpl implements ImportTaskRepository {

    private final ImportTaskMapper mapper;

    @Override
    public Optional<ImportTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<ImportTask> findByNo(String taskNo) {
        return new DbRepositorySupport<>(mapper, ImportTask.class)
                .findOne(new LambdaQueryWrapper<ImportTask>().eq(ImportTask::getTaskNo, taskNo));
    }

    @Override
    public void save(ImportTask task) {
        new DbRepositorySupport<>(mapper, ImportTask.class).save(task);
    }

    @Override
    public void update(ImportTask task) {
        new DbRepositorySupport<>(mapper, ImportTask.class).update(task);
    }

    @Override
    public List<ImportTask> listByStatus(String status) {
        return mapper.selectList(new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getStatus, status)
                .orderByAsc(ImportTask::getTaskNo));
    }

    @Override
    public List<ImportTask> listByType(String importType) {
        return mapper.selectList(new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getImportType, importType)
                .orderByAsc(ImportTask::getTaskNo));
    }

    @Override
    public List<ImportTask> listByStatusAndType(String status, String importType) {
        return mapper.selectList(new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getStatus, status)
                .eq(ImportTask::getImportType, importType)
                .orderByAsc(ImportTask::getTaskNo));
    }

    @Override
    public List<ImportTask> listByOperator(String operator) {
        return mapper.selectList(new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getOperator, operator)
                .orderByAsc(ImportTask::getTaskNo));
    }

    @Override
    public List<ImportTask> listByOperatorAndStatus(String operator, String status) {
        return mapper.selectList(new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getOperator, operator)
                .eq(ImportTask::getStatus, status)
                .orderByAsc(ImportTask::getTaskNo));
    }
}
