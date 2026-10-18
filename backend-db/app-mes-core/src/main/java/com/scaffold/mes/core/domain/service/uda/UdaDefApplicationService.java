package com.scaffold.mes.core.domain.service.uda;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.uda.UdaDefCreateRequest;
import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.repository.uda.UdaDefRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * UdaDefApplicationService 自定义属性定义
 * <p>
 * 同一对象类型下属性编码不能重复。ENUM 必须带选项。新建时启用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UdaDefApplicationService {

    private static final Set<String> OBJECTS = Set.of("LOT", "WAFER", "PRODUCT", "EQUIPMENT", "MATERIAL", "WORK_ORDER");

    private static final Set<String> TYPES = Set.of("TEXT", "NUMBER", "DATE", "BOOL", "ENUM");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final UdaDefRepository repository;

    /**
     * 列出某个对象类型的定义。
     *
     * @param objectType 对象类型
     * @return 按属性编码排序。类型为空或不认识时为空列表
     */
    public List<UdaDef> list(String objectType) {
        if (objectType == null || objectType.isBlank() || !OBJECTS.contains(objectType)) {
            return List.of();
        }
        return repository.listByObject(objectType);
    }

    /**
     * 新建定义。
     *
     * @param request 对象类型、编码、名称和数据类型
     * @throws BusinessException 编码已存在，或对象类型、名称、数据类型不合法
     */
    @Transactional
    public void create(UdaDefCreateRequest request) {
        if (request.getAttrName() == null || request.getAttrName().isBlank() || !OBJECTS.contains(request.getObjectType())) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "对象类型或属性名称不合法");
        }
        String dataType = request.getDataType();
        if (dataType == null || dataType.isBlank()) {
            dataType = "TEXT";
        } else if (!TYPES.contains(dataType)) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "数据类型只能是 TEXT、NUMBER、DATE、BOOL 或 ENUM");
        }
        Integer required = request.getRequired() == null ? 0 : request.getRequired();
        if (required != 0 && required != 1) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "必填标记只能是 0 或 1");
        }
        if (repository.findByObjectAndCode(request.getObjectType(), request.getAttrCode()).isPresent()) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "属性编码已存在");
        }
        String optionsJson = null;
        if ("ENUM".equals(dataType)) {
            optionsJson = options(request.getOptions());
        }
        UdaDef def = new UdaDef();
        def.setObjectType(request.getObjectType());
        def.setAttrCode(request.getAttrCode());
        def.setAttrName(request.getAttrName());
        def.setDataType(dataType);
        def.setOptionsJson(optionsJson);
        def.setRequired(required);
        def.setEnabled(1);
        repository.save(def);
    }

    private static String options(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "枚举必须填写选项");
        }
        List<String> values = new ArrayList<>();
        for (String piece : raw.split(",")) {
            String option = piece.trim();
            if (option.isEmpty()) {
                throw new BusinessException(MesErrorCodes.UDA_INVALID, "枚举必须填写选项");
            }
            values.add(option);
        }
        try {
            return JSON.writeValueAsString(values);
        } catch (Exception ex) {
            throw new BusinessException(MesErrorCodes.UDA_INVALID, "枚举选项无法保存");
        }
    }
}
