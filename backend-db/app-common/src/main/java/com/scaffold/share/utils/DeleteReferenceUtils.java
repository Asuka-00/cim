package com.scaffold.share.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.scaffold.share.exception.BusinessException;

/**
 * 删除前引用校验工具。
 * <p>
 * 存在引用时抛出 {@link BusinessException}，并附带 {@code errorParams} 供前端 i18n 插值：
 * <ul>
 *   <li>{@code target} — 被删目标名称</li>
 *   <li>{@code boundBy} — 引用方名称列表（逗号分隔）</li>
 *   <li>{@code details} — 引用方 + 条数的可读摘要</li>
 * </ul>
 */
public final class DeleteReferenceUtils {

    private DeleteReferenceUtils() {
    }

    /**
     * 若任一引用 count &gt; 0，则抛出业务异常。
     *
     * @param errorCode  业务错误码
     * @param targetName 被删除对象的展示名（如「租户」「角色」）
     * @param references 引用统计
     */
    public static void requireNoReferences(int errorCode, String targetName, Reference... references) {
        var matchedReferences = Arrays.stream(references)
                .filter(Objects::nonNull)
                .filter(reference -> reference.count() > 0)
                .toList();
        if (matchedReferences.isEmpty()) {
            return;
        }

        String details = matchedReferences.stream()
                .map(reference -> reference.name() + " " + reference.count() + " 条")
                .collect(Collectors.joining("，"));
        String boundBy = matchedReferences.stream()
                .map(Reference::name)
                .collect(Collectors.joining(", "));
        String safeTarget = targetName != null ? targetName : "";

        throw new BusinessException(
                errorCode,
                safeTarget + "已被以下数据使用，无法删除：" + details,
                Map.of(
                        "target", safeTarget,
                        "boundBy", boundBy,
                        "details", details));
    }

    public static Reference reference(String name, long count) {
        return new Reference(name, count);
    }

    public record Reference(String name, long count) {
    }
}
