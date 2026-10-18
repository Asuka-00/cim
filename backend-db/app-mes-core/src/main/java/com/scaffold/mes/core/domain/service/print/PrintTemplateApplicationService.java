package com.scaffold.mes.core.domain.service.print;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.print.PrintTemplateCreateRequest;
import com.scaffold.mes.core.domain.dto.print.PrintTemplateResponse;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.repository.print.PrintTemplateRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * PrintTemplateApplicationService 打印模板
 * <p>
 * 编码不能重复。新建时启用。同一类型只保留一个默认模板。停用后按类型查不到它。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class PrintTemplateApplicationService {

    private static final Set<String> TYPES = Set.of(
            "LOT_TRAVELER", "CARRIER_LABEL", "WAFER_LABEL", "STRIP_LABEL", "REEL_LABEL", "MATERIAL_LABEL");

    private final PrintTemplateRepository repository;

    /**
     * 按类型列出模板。
     *
     * @param tplType 模板类型
     * @return 该类型的模板。类型为空或不认识时为空列表
     */
    public List<PrintTemplate> list(String tplType) {
        if (tplType == null || tplType.isBlank() || !TYPES.contains(tplType)) {
            return List.of();
        }
        return repository.listByType(tplType);
    }

    /**
     * 新建模板。
     *
     * @param request 编码、名称、类型和内容
     * @throws BusinessException 编码已存在，或名称、类型、内容不合法
     */
    @Transactional
    public void create(PrintTemplateCreateRequest request) {
        if (request.getTplName() == null || request.getTplName().isBlank()
                || request.getContent() == null || request.getContent().isBlank()
                || !TYPES.contains(request.getTplType())) {
            throw new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "模板名称、类型或内容不合法");
        }
        if (repository.findByCode(request.getTplCode()).isPresent()) {
            throw new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "模板编码已存在");
        }
        PrintTemplate template = new PrintTemplate();
        template.setTplCode(request.getTplCode());
        template.setTplName(request.getTplName());
        template.setTplType(request.getTplType());
        template.setContent(request.getContent());
        template.setEnabled(1);
        template.setIsDefault(0);
        repository.save(template);
    }

    /**
     * 设成该类型的默认模板，并清掉同类型的其他默认。
     *
     * @param id 模板
     * @throws BusinessException 模板不存在或已停用
     */
    @Transactional
    public void setDefault(Long id) {
        PrintTemplate template = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "打印模板不存在"));
        if (!Integer.valueOf(1).equals(template.getEnabled())) {
            throw new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "停用的模板不能设成默认");
        }
        for (PrintTemplate row : repository.listByType(template.getTplType())) {
            if (row.getId().equals(id) || !Integer.valueOf(1).equals(row.getIsDefault())) {
                continue;
            }
            row.setIsDefault(0);
            repository.update(row);
        }
        template.setIsDefault(1);
        repository.update(template);
    }

    /**
     * 按类型查出已启用的默认模板。
     *
     * @param tplType 模板类型
     * @return 编码、名称和内容
     * @throws BusinessException 这个类型没有默认模板
     */
    public PrintTemplateResponse showDefault(String tplType) {
        PrintTemplate template = repository.findDefault(tplType)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "这个类型没有默认模板"));
        return new PrintTemplateResponse(template.getTplCode(), template.getTplName(), template.getContent());
    }

    /**
     * 停用模板，并取消它的默认标记。
     *
     * @param id 模板
     * @throws BusinessException 模板不存在
     */
    @Transactional
    public void disable(Long id) {
        PrintTemplate template = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "打印模板不存在"));
        template.setEnabled(0);
        template.setIsDefault(0);
        repository.update(template);
    }
}
