package com.scaffold.system.domain.service.system;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;
import com.scaffold.share.utils.JsonWriter;
import com.scaffold.share.utils.PageQueryUtils;
import com.scaffold.system.domain.converter.system.DictItemConverter;
import com.scaffold.system.domain.converter.system.DictTypeConverter;
import com.scaffold.system.domain.dto.system.DictItemCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictItemUpdateRequestDTO;
import com.scaffold.system.domain.dto.system.DictTypeCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictTypeUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.DictItem;
import com.scaffold.system.domain.entity.system.DictType;
import com.scaffold.system.domain.query.system.DictItemQuery;
import com.scaffold.system.domain.query.system.DictTypeQuery;
import com.scaffold.system.domain.repository.system.DictItemRepository;
import com.scaffold.system.domain.repository.system.DictTypeRepository;
import com.scaffold.system.domain.vo.system.DictItemVO;
import com.scaffold.system.domain.vo.system.DictTypeVO;

import lombok.RequiredArgsConstructor;

/**
 * DictApplicationService 应用服务类
 * <p>
 * 负责编排字典类型和字典选项的管理逻辑，包括编码唯一性校验、删除引用保护和启用选项查询。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Service
@RequiredArgsConstructor
public class DictApplicationService {

    private static final String ATTR_LABELS = "labels";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("zh", "en");
    private static final TypeReference<Map<String, Object>> ATTRS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> LABELS_TYPE = new TypeReference<>() {
    };

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;
    private final DictTypeConverter dictTypeConverter;
    private final DictItemConverter dictItemConverter;
    private final JsonWriter jsonWriter;
    private final ObjectMapper objectMapper;

    /**
     * 新增字典类型
     */
    @Transactional
    public void addDictType(DictTypeCreateRequestDTO request) {
        dictTypeRepository.findByDictType(request.getDictType()).ifPresent(existing -> {
            throw new BusinessException(ErrorCodeConstants.DICT_TYPE_EXISTS, "字典类型已存在: " + request.getDictType());
        });
        dictTypeRepository.save(dictTypeConverter.toEntity(request));
    }

    /**
     * 更新字典类型
     */
    @Transactional
    public void updateDictType(Long id, DictTypeUpdateRequestDTO request) {
        DictType dictType = dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_TYPE_NOT_FOUND, "字典类型不存在: " + id));
        dictTypeConverter.updateEntity(request, dictType);
        dictTypeRepository.update(dictType);
    }

    /**
     * 删除字典类型
     * <p>
     * 删除前检查是否仍有关联字典选项，避免留下无归属的选项数据。
     */
    @Transactional
    public void deleteDictType(Long id) {
        DictType dictType = dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_TYPE_NOT_FOUND, "字典类型不存在: " + id));
        DeleteReferenceUtils.requireNoReferences(ErrorCodeConstants.DICT_TYPE_HAS_BEEN_BOUND, "字典类型",
                DeleteReferenceUtils.reference("字典选项",
                        dictItemRepository.findByDictType(dictType.getDictType()).size()));
        dictTypeRepository.deleteById(id);
    }

    /**
     * 根据ID查询字典类型
     */
    public DictTypeVO getDictType(Long id) {
        return dictTypeConverter.toVO(dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_TYPE_NOT_FOUND, "字典类型不存在: " + id)));
    }

    /**
     * 分页查询字典类型
     */
    public PageDTO<DictTypeVO> queryDictTypes(int page, int pageSize, DictTypeQuery query) {
        return PageQueryUtils.map(dictTypeRepository.queryPage(page, pageSize, query), dictTypeConverter::toVO);
    }

    /**
     * 查询所有字典类型（用于下拉选择）
     */
    public List<DictTypeVO> listAllDictTypes() {
        return dictTypeConverter.toVOList(dictTypeRepository.findAll());
    }

    /**
     * 新增字典选项
     */
    @Transactional
    public void addDictItem(DictItemCreateRequestDTO request) {
        dictTypeRepository.findByDictType(request.getDictType())
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_TYPE_NOT_FOUND,
                        "字典类型不存在: " + request.getDictType()));
        dictItemRepository.findByDictTypeAndItemCode(request.getDictType(), request.getItemCode())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodeConstants.DICT_ITEM_EXISTS,
                            "字典选项已存在: " + request.getDictType() + "/" + request.getItemCode());
                });
        DictItem entity = dictItemConverter.toEntity(request);
        applyLabels(entity, request.getLabels(), request.getItemLabel());
        dictItemRepository.save(entity);
    }

    /**
     * 更新字典选项
     */
    @Transactional
    public void updateDictItem(Long id, DictItemUpdateRequestDTO request) {
        DictItem item = dictItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_ITEM_NOT_FOUND, "字典选项不存在: " + id));
        dictItemConverter.updateEntity(request, item);
        applyLabels(item, request.getLabels(), request.getItemLabel());
        dictItemRepository.update(item);
    }

    /**
     * 删除字典选项
     */
    @Transactional
    public void deleteDictItem(Long id) {
        dictItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_ITEM_NOT_FOUND, "字典选项不存在: " + id));
        dictItemRepository.deleteById(id);
    }

    /**
     * 根据ID查询字典选项
     */
    public DictItemVO getDictItem(Long id) {
        return toItemVO(dictItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.DICT_ITEM_NOT_FOUND, "字典选项不存在: " + id)));
    }

    /**
     * 分页查询字典选项
     */
    public PageDTO<DictItemVO> queryDictItems(int page, int pageSize, DictItemQuery query) {
        return PageQueryUtils.map(dictItemRepository.queryPage(page, pageSize, query), this::toItemVO);
    }

    /**
     * 根据字典类型查询启用选项
     * <p>
     * 返回结果按 sortOrder 升序排列，未配置排序号的选项放在末尾。
     */
    public List<DictItemVO> getEnabledItems(String dictType) {
        return dictItemRepository.findByDictType(dictType).stream()
                .filter(item -> StateEnum.ENABLED.equals(item.getEnabled()))
                .sorted(Comparator.comparing(DictItem::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toItemVO)
                .toList();
    }

    private DictItemVO toItemVO(DictItem entity) {
        DictItemVO vo = dictItemConverter.toVO(entity);
        Map<String, String> labels = parseLabelsFromAttrs(entity.getAttrsJson());
        if (labels.isEmpty() && StringUtils.hasText(entity.getItemLabel())) {
            labels = new LinkedHashMap<>();
            labels.put("zh", entity.getItemLabel());
        }
        vo.setLabels(labels);
        if (!StringUtils.hasText(vo.getItemLabel()) && labels.containsKey("zh")) {
            vo.setItemLabel(labels.get("zh"));
        }
        return vo;
    }

    private void applyLabels(DictItem entity, Map<String, String> labels, String itemLabel) {
        Map<String, String> normalized = normalizeLabels(labels, itemLabel);
        String zh = normalized.get("zh");
        if (!StringUtils.hasText(zh)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "字典显示名 labels.zh 不能为空");
        }
        if (zh.length() > 128) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "字典显示名长度不能超过128");
        }
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() > 128) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM,
                        "字典显示名长度不能超过128: " + entry.getKey());
            }
        }
        entity.setItemLabel(zh);
        Map<String, Object> attrs = parseAttrs(entity.getAttrsJson());
        attrs.put(ATTR_LABELS, normalized);
        entity.setAttrsJson(jsonWriter.write(attrs));
    }

    private Map<String, String> normalizeLabels(Map<String, String> labels, String itemLabel) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String locale = entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (!SUPPORTED_LOCALES.contains(locale)) {
                    continue;
                }
                if (!StringUtils.hasText(entry.getValue())) {
                    continue;
                }
                normalized.put(locale, entry.getValue().trim());
            }
        }
        if (!normalized.containsKey("zh") && StringUtils.hasText(itemLabel)) {
            normalized.put("zh", itemLabel.trim());
        }
        return normalized;
    }

    private Map<String, Object> parseAttrs(String attrsJson) {
        if (!StringUtils.hasText(attrsJson)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(attrsJson, ATTRS_TYPE);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, String> parseLabelsFromAttrs(String attrsJson) {
        Map<String, Object> attrs = parseAttrs(attrsJson);
        Object labelsNode = attrs.get(ATTR_LABELS);
        if (labelsNode == null) {
            // 兼容旧 labels_json 扁平结构：{"zh":"...","en":"..."}
            if (attrs.keySet().stream().anyMatch(SUPPORTED_LOCALES::contains)) {
                return normalizeLabels(toStringMap(attrs), null);
            }
            return new LinkedHashMap<>();
        }
        if (labelsNode instanceof Map<?, ?> map) {
            return normalizeLabels(toStringMap(map), null);
        }
        try {
            Map<String, String> parsed = objectMapper.convertValue(labelsNode, LABELS_TYPE);
            return normalizeLabels(parsed, null);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, String> toStringMap(Map<?, ?> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return result;
    }
}
