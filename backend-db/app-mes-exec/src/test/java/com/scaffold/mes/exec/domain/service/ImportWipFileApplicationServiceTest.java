package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.mes.core.domain.service.file.MesFileApplicationService;
import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.share.exception.BusinessException;

class ImportWipFileApplicationServiceTest {

    private ImportTaskRepository tasks;
    private MesFileApplicationService files;
    private ImportWipRowApplicationService rows;
    private ImportWipFileApplicationService service;

    @BeforeEach
    void setUp() {
        tasks = mock(ImportTaskRepository.class);
        files = mock(MesFileApplicationService.class);
        rows = mock(ImportWipRowApplicationService.class);
        service = new ImportWipFileApplicationService(tasks, files, rows);
    }

    @Test
    void importsRowsFromStoredCsv() {
        ImportTask task = new ImportTask();
        task.setFileRef("9");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(files.load(9L)).thenReturn("woId,lotNo,qty,seqNo,stepId\n4,L-1,1,1,7\n".getBytes(StandardCharsets.UTF_8));

        service.importStored(7L);

        verify(rows).importRows(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<java.util.List<ImportWipRowRequest>>argThat(parsed ->
                        parsed.size() == 1
                                && Long.valueOf(4L).equals(parsed.get(0).getWoId())
                                && "L-1".equals(parsed.get(0).getRow().getLotNo())
                                && Integer.valueOf(1).equals(parsed.get(0).getRowNo())));
    }

    @Test
    void rejectsMissingFileRef() {
        ImportTask task = new ImportTask();
        when(tasks.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.importStored(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(400190, "导入文件不存在");
        verify(files, never()).load(org.mockito.ArgumentMatchers.any());
        verify(rows, never()).importRows(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
