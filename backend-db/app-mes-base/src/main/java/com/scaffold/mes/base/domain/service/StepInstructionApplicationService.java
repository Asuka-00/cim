package com.scaffold.mes.base.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepInstructionResponse;
import com.scaffold.mes.base.domain.dto.StepInstructionSaveRequest;
import com.scaffold.mes.base.domain.entity.StepInstruction;
import com.scaffold.mes.base.domain.repository.StepInstructionRepository;
import com.scaffold.mes.core.domain.repository.file.MesFileRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StepInstructionApplicationService 站点作业指导
 * <p>
 * 正文不能空。取出时产品专用优先，停用的指导跳过，再看通用指导。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepInstructionApplicationService {

    private final StepInstructionRepository repository;
    private final MesFileRepository files;

    /**
     * 保存指导。
     *
     * @param request 站点、产品和正文
     * @throws BusinessException 正文为空，或该站点与产品已有指导
     */
    @Transactional
    public void save(StepInstructionSaveRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID, "作业指导正文不能为空");
        }
        if (request.getFileId() != null && files.findById(request.getFileId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID, "附件文件不存在");
        }
        long productId = request.getProductId() == null ? 0L : request.getProductId();
        if (repository.find(request.getStepId(), productId).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_INSTRUCTION_INVALID, "这个站点与产品已有作业指导");
        }
        StepInstruction instruction = new StepInstruction();
        instruction.setStepId(request.getStepId());
        instruction.setProductId(productId);
        instruction.setTitle(request.getTitle());
        instruction.setContent(request.getContent());
        instruction.setFileId(request.getFileId());
        instruction.setEnabled(1);
        repository.save(instruction);
    }

    /**
     * 取出这个站点和产品要展示的指导。
     *
     * @param stepId 站点
     * @param productId 产品。空按通用
     * @return 指导。都没有或都停用时为空
     */
    public Optional<StepInstructionResponse> show(Long stepId, Long productId) {
        long product = productId == null ? 0L : productId;
        StepInstruction specific = product == 0L ? null : enabled(repository.find(stepId, product).orElse(null));
        if (specific != null) {
            return Optional.of(response(specific));
        }
        StepInstruction general = enabled(repository.find(stepId, 0L).orElse(null));
        if (general == null) {
            return Optional.empty();
        }
        return Optional.of(response(general));
    }

    private static StepInstructionResponse response(StepInstruction instruction) {
        return new StepInstructionResponse(instruction.getTitle(), instruction.getContent(), instruction.getFileId());
    }

    private static StepInstruction enabled(StepInstruction instruction) {
        if (instruction == null || !Integer.valueOf(1).equals(instruction.getEnabled())) {
            return null;
        }
        return instruction;
    }
}
