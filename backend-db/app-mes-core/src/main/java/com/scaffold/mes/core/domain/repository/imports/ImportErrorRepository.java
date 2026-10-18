package com.scaffold.mes.core.domain.repository.imports;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.imports.ImportError;

/**
 * ImportErrorRepository 导入错误明细仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ImportErrorRepository {

    /**
     * 追加一条错误。
     *
     * @param error 错误明细
     */
    void save(ImportError error);

    /**
     * 按行号列出这个任务的失败行。
     *
     * @param taskId 任务
     * @return 失败行
     */
    List<ImportError> listByTask(Long taskId);

    /**
     * 这个任务是否已经记过该行号。
     *
     * @param taskId 任务
     * @param rowNo 行号
     * @return 已存在时为 true
     */
    boolean exists(Long taskId, Integer rowNo);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 错误明细，不存在时为空
     */
    Optional<ImportError> findById(Long id);

    /**
     * 再次新建草稿时清掉这个任务原来的失败行。
     *
     * @param taskId 任务
     */
    void deleteByTask(Long taskId);
}
