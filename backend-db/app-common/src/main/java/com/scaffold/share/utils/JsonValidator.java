package com.scaffold.share.utils;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;

/**
 * JsonValidator JSON 文本合法性校验器。
 * <p>
 * 自持一个 {@link ObjectMapper}，对外提供 {@link #validateJson(String)}：空值视为合法直接跳过，
 * 否则尝试解析，失败抛出业务异常。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-17
 */
@Component
public class JsonValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 校验字符串是否为合法 JSON，空值视为合法直接跳过。
     *
     * @param json 待校验的 JSON 字符串
     * @throws BusinessException 若 JSON 格式非法
     */
    public void validateJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "JSON 格式非法", ex);
        }
    }
}
