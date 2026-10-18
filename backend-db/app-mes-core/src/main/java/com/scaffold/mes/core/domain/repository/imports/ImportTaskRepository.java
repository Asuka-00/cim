package com.scaffold.mes.core.domain.repository.imports;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.imports.ImportTask;

/**
 * ImportTaskRepository 导入任务仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ImportTaskRepository {

    /**
     * 按任务号查询。
     *
     * @param taskNo 任务号
     * @return 任务，不存在时为空
     */
    Optional<ImportTask> findByNo(String taskNo);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 任务，不存在时为空
     */
    Optional<ImportTask> findById(Long id);

    /**
     * 插入。
     *
     * @param task 任务
     */
    void save(ImportTask task);

    /**
     * 更新。
     *
     * @param task 任务
     */
    void update(ImportTask task);

    /**
     * 按状态列出，按任务号升序。
     *
     * @param status 状态
     * @return 任务
     */
    List<ImportTask> listByStatus(String status);

    /**
     * 按导入类型列出，按任务号升序。
     *
     * @param importType 导入类型
     * @return 任务
     */
    List<ImportTask> listByType(String importType);

    /**
     * 按状态和导入类型列出，按任务号升序。
     *
     * @param status 状态
     * @param importType 导入类型
     * @return 任务
     */
    List<ImportTask> listByStatusAndType(String status, String importType);

    /**
     * 按操作人列出，按任务号升序。
     *
     * @param operator 操作人
     * @return 任务
     */
    List<ImportTask> listByOperator(String operator);

    /**
     * 按操作人和状态列出，按任务号升序。
     *
     * @param operator 操作人
     * @param status 状态
     * @return 任务
     */
    List<ImportTask> listByOperatorAndStatus(String operator, String status);
}
