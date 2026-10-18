package com.scaffold.mes.exec.domain.service;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.mes.core.domain.service.file.MesFileApplicationService;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ImportWipFileApplicationService 按任务上的文件导入期初在制
 * <p>
 * fileRef 是 mes_file 的编号。内容须是 CSV，解析后交给逐行写入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ImportWipFileApplicationService {

    private final ImportTaskRepository tasks;

    private final MesFileApplicationService files;

    private final ImportWipRowApplicationService rows;

    /**
     * 读出任务引用的文件并逐行导入。
     *
     * @param taskId 任务
     * @throws BusinessException 任务或文件引用不存在，或 CSV 无法解析
     */
    public void importStored(Long taskId) {
        ImportTask task = tasks.findById(taskId)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        String ref = task.getFileRef();
        if (ref == null || ref.isEmpty() || !ref.chars().allMatch(Character::isDigit)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入文件不存在");
        }
        long fileId;
        try {
            fileId = Long.parseLong(ref);
        } catch (NumberFormatException ex) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入文件不存在");
        }
        String csv = new String(files.load(fileId), StandardCharsets.UTF_8);
        rows.importRows(taskId, WipImportCsv.parse(csv));
    }
}
