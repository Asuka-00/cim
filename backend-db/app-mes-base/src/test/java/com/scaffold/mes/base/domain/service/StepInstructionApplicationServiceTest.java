package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepInstructionResponse;
import com.scaffold.mes.base.domain.dto.StepInstructionSaveRequest;
import com.scaffold.mes.base.domain.entity.StepInstruction;
import com.scaffold.mes.base.domain.repository.StepInstructionRepository;
import com.scaffold.mes.core.domain.entity.file.MesFile;
import com.scaffold.mes.core.domain.repository.file.MesFileRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 站点可以挂一段作业指导。正文为空或已经有指导时拒绝。
 */
class StepInstructionApplicationServiceTest {

    private StepInstructionRepository repository;
    private MesFileRepository files;
    private StepInstructionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepInstructionRepository.class);
        files = mock(MesFileRepository.class);
        when(repository.find(7L, 0L)).thenReturn(Optional.empty());
        service = new StepInstructionApplicationService(repository, files);
    }

    @Test
    void savesTextForTheStep() {
        StepInstructionSaveRequest request = request("先核对批次号");
        service.save(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<StepInstruction>argThat(row ->
                row.getStepId().equals(7L) && row.getProductId().equals(0L)
                        && "先核对批次号".equals(row.getContent()) && row.getEnabled().equals(1)));
    }

    @Test
    void blankContentRejected() {
        assertThatThrownBy(() -> service.save(request("  ")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID);
    }

    @Test
    void duplicateStepRejected() {
        when(repository.find(7L, 0L)).thenReturn(Optional.of(new StepInstruction()));

        assertThatThrownBy(() -> service.save(request("再写一条")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID);
    }

    @Test
    void productInstructionBeatsGeneral() {
        when(repository.find(7L, 2L)).thenReturn(Optional.of(instruction("产品专用", 1)));
        when(repository.find(7L, 0L)).thenReturn(Optional.of(instruction("通用", 1)));

        assertThat(service.show(7L, 2L)).contains(new StepInstructionResponse("进站前", "产品专用", null));
    }

    @Test
    void disabledProductFallsBackToGeneral() {
        when(repository.find(7L, 2L)).thenReturn(Optional.of(instruction("产品专用", 0)));
        when(repository.find(7L, 0L)).thenReturn(Optional.of(instruction("通用", 1)));

        assertThat(service.show(7L, 2L)).contains(new StepInstructionResponse("进站前", "通用", null));
    }

    @Test
    void attachesRegisteredFile() {
        when(files.findById(3L)).thenReturn(Optional.of(new MesFile()));
        StepInstructionSaveRequest request = request("先核对批次号");
        request.setFileId(3L);

        service.save(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<StepInstruction>argThat(row -> row.getFileId().equals(3L)));
    }

    @Test
    void missingFileRejected() {
        when(files.findById(3L)).thenReturn(Optional.empty());
        StepInstructionSaveRequest request = request("先核对批次号");
        request.setFileId(3L);

        assertThatThrownBy(() -> service.save(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID);
    }

    private static StepInstruction instruction(String content, int enabled) {
        StepInstruction instruction = new StepInstruction();
        instruction.setTitle("进站前");
        instruction.setContent(content);
        instruction.setEnabled(enabled);
        return instruction;
    }

    private static StepInstructionSaveRequest request(String content) {
        StepInstructionSaveRequest request = new StepInstructionSaveRequest();
        request.setStepId(7L);
        request.setTitle("进站前");
        request.setContent(content);
        return request;
    }
}
