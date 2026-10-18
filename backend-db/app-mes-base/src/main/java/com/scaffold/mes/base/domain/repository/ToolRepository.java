package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.Tool;

/**
 * ToolRepository 工具仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ToolRepository {

    /**
     * 按编码查询。
     *
     * @param toolCode 编码
     * @return 工具，不存在时为空
     */
    Optional<Tool> findByCode(String toolCode);

    /**
     * 按主键查询。
     *
     * @param id 工具
     * @return 工具，不存在时为空
     */
    Optional<Tool> findById(Long id);

    /**
     * 列出全部工具，按编码。
     *
     * @return 工具
     */
    List<Tool> list();

    /**
     * 插入。
     *
     * @param tool 工具
     */
    void save(Tool tool);

    /**
     * 更新。领用时改状态和设备。
     *
     * @param tool 已加载的工具
     */
    void update(Tool tool);
}
