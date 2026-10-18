package com.scaffold.mes.core.domain.repository.print;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.print.PrintTemplate;

/**
 * PrintTemplateRepository 打印模板仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface PrintTemplateRepository {

    /**
     * 按编码查询。
     *
     * @param tplCode 模板编码
     * @return 模板，不存在时为空
     */
    Optional<PrintTemplate> findByCode(String tplCode);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 模板，不存在时为空
     */
    Optional<PrintTemplate> findById(Long id);

    /**
     * 按类型列出模板。
     *
     * @param tplType 模板类型
     * @return 该类型的模板。没有时为空列表
     */
    List<PrintTemplate> listByType(String tplType);

    /**
     * 按类型查已启用的默认模板。
     *
     * @param tplType 模板类型
     * @return 默认模板，没有时为空
     */
    Optional<PrintTemplate> findDefault(String tplType);

    /**
     * 插入。
     *
     * @param template 模板
     */
    void save(PrintTemplate template);

    /**
     * 更新。
     *
     * @param template 模板
     */
    void update(PrintTemplate template);
}
