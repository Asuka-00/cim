package com.scaffold.mes.exec.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.repository.uda.UdaDefRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotUdaPutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotUdaApplicationService Lot 自定义属性
 * <p>
 * 属性必须是已启用的 LOT 定义。NUMBER、BOOL、DATE、ENUM 按各自规则校验。可以查出还缺哪些必填属性。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotUdaApplicationService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };

    private static final TypeReference<List<String>> OPTIONS = new TypeReference<>() {
    };

    private final LotRepository lots;

    private final UdaDefRepository definitions;

    /**
     * 写入一个属性值。
     *
     * @param lotId Lot
     * @param request 属性编码和值
     * @throws BusinessException Lot 不存在，属性没定义或已停用，值不符合数据类型，或已有内容不是对象
     */
    @Transactional
    public void put(Long lotId, LotUdaPutRequest request) {
        if (request.getValue() == null || request.getValue().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "属性值不能为空");
        }
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        UdaDef def = definitions.findByObjectAndCode("LOT", request.getAttrCode()).orElse(null);
        if (def == null || !Integer.valueOf(1).equals(def.getEnabled())) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "自定义属性没定义");
        }
        if ("NUMBER".equals(def.getDataType()) && !request.getValue().matches("-?\\d+(\\.\\d+)?")) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "数字属性的值必须是数字");
        }
        if ("BOOL".equals(def.getDataType()) && !"true".equals(request.getValue()) && !"false".equals(request.getValue())) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "布尔属性的值只能是 true 或 false");
        }
        if ("DATE".equals(def.getDataType()) && !date(request.getValue())) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "日期属性的值必须是 yyyy-MM-dd");
        }
        if ("ENUM".equals(def.getDataType()) && !options(def.getOptionsJson()).contains(request.getValue())) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "枚举属性的值必须在选项里");
        }
        LinkedHashMap<String, String> values = read(lot.getExtJson());
        values.put(request.getAttrCode(), request.getValue());
        try {
            lot.setExtJson(JSON.writeValueAsString(values));
        } catch (Exception ex) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "属性值无法保存");
        }
        lots.update(lot);
    }

    /**
     * 读出已写入的属性。
     *
     * @param lotId Lot
     * @return 属性编码到值。还没写过时为空
     * @throws BusinessException Lot 不存在，或已有内容不是对象
     */
    public Map<String, String> show(Long lotId) {
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        return read(lot.getExtJson());
    }

    /**
     * 查出还没写的必填属性编码。
     *
     * @param lotId Lot
     * @return 缺失的属性编码。都写过时为空
     * @throws BusinessException Lot 不存在，或已有内容不是对象
     */
    public List<String> missing(Long lotId) {
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        Map<String, String> values = read(lot.getExtJson());
        List<String> missing = new ArrayList<>();
        for (UdaDef def : definitions.listRequired("LOT")) {
            String value = values.get(def.getAttrCode());
            if (value == null || value.isBlank()) {
                missing.add(def.getAttrCode());
            }
        }
        return missing;
    }

    private static List<String> options(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(optionsJson, OPTIONS);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static boolean date(String value) {
        try {
            return value.equals(LocalDate.parse(value).toString());
        } catch (Exception ex) {
            return false;
        }
    }

    private static LinkedHashMap<String, String> read(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.readValue(extJson, MAP);
        } catch (Exception ex) {
            throw new BusinessException(MesExecErrorCodes.UDA_VALUE, "已有自定义属性不是对象");
        }
    }
}
