package com.scaffold.mes.base.domain.repository;

import com.scaffold.mes.base.domain.entity.ToolEvent;

/**
 * ToolEventRepository 工具事件仓储
 * <p>
 * 只追加 hist_mes_tool_event。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ToolEventRepository {

    /**
     * 插入一条事件。
     *
     * @param event 事件
     */
    void save(ToolEvent event);
}
