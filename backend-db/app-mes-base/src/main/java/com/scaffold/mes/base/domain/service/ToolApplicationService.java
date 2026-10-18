package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ToolCheckoutRequest;
import com.scaffold.mes.base.domain.dto.ToolCreateRequest;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.entity.ToolEvent;
import com.scaffold.mes.base.domain.repository.ToolEventRepository;
import com.scaffold.mes.base.domain.repository.ToolRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ToolApplicationService 工具领用
 * <p>
 * 新建的工具是 AVAILABLE。领用后装到设备，状态变为 IN_USE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ToolApplicationService {

    private final ToolRepository repository;
    private final ToolEventRepository events;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 列出全部工具。
     *
     * @return 按编码排列
     */
    public List<Tool> list() {
        return repository.list();
    }

    /**
     * 新建可用工具。
     *
     * @param request 编码、名称和类型
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(ToolCreateRequest request) {
        if (repository.findByCode(request.getToolCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.TOOL_CODE_EXISTS, "工具编码已存在");
        }
        Tool tool = new Tool();
        tool.setToolCode(request.getToolCode());
        tool.setToolName(request.getToolName());
        tool.setTypeId(request.getTypeId());
        tool.setStatus("AVAILABLE");
        tool.setUsedCount(0L);
        tool.setEnabled(1);
        tool.setVersion(0);
        repository.save(tool);
    }

    /**
     * 领用到设备。
     *
     * @param toolId 工具
     * @param request 设备
     * @throws BusinessException 工具不存在、未启用，或不是可用状态
     */
    @Transactional
    public void checkout(Long toolId, ToolCheckoutRequest request) {
        Tool tool = repository.findById(toolId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.TOOL_NOT_AVAILABLE, "工具不存在"));
        if (tool.getEnabled() == null || tool.getEnabled() != 1 || !"AVAILABLE".equals(tool.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.TOOL_NOT_AVAILABLE, "只有可用的工具可以领用");
        }
        tool.setStatus("IN_USE");
        tool.setCurrentEquipmentId(request.getEquipmentId());
        repository.update(tool);
    }

    /**
     * 归还工具。离开设备，状态回到可用。
     *
     * @param toolId 工具
     * @throws BusinessException 工具不存在，或不是使用中
     */
    @Transactional
    public void returnTool(Long toolId) {
        Tool tool = repository.findById(toolId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.TOOL_NOT_IN_USE, "工具不存在"));
        if (!"IN_USE".equals(tool.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.TOOL_NOT_IN_USE, "只有使用中的工具可以归还");
        }
        tool.setStatus("AVAILABLE");
        tool.setCurrentEquipmentId(null);
        repository.update(tool);
    }

    /**
     * 使用量是否还没到寿命上限。
     *
     * @param toolId 工具
     * @return 没有上限，或使用量小于上限时为 true
     */
    public boolean withinLife(Long toolId) {
        Tool tool = repository.findById(toolId).orElse(null);
        if (tool == null || tool.getLifeLimit() == null) {
            return tool != null;
        }
        long used = tool.getUsedCount() == null ? 0L : tool.getUsedCount();
        return used < tool.getLifeLimit();
    }

    /**
     * 使用量加 1。达到上限时改为保养。
     *
     * @param toolId 工具
     * @throws BusinessException 工具不存在
     */
    @Transactional
    public void use(Long toolId) {
        Tool tool = repository.findById(toolId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.TOOL_NOT_FOUND, "工具不存在"));
        long used = tool.getUsedCount() == null ? 0L : tool.getUsedCount();
        long next = used + 1;
        tool.setUsedCount(next);
        if (tool.getLifeLimit() != null && next >= tool.getLifeLimit()) {
            tool.setStatus("MAINTENANCE");
        }
        repository.update(tool);
        write(tool, "USE", next);
        if (crossedNinety(used, next, tool.getLifeLimit())) {
            write(tool, "LIFE_WARN", next);
        }
    }

    private static boolean crossedNinety(long used, long next, Long limit) {
        if (limit == null || limit < 1) {
            return false;
        }
        return used * 10 < limit * 9 && next * 10 >= limit * 9;
    }

    private void write(Tool tool, String type, long total) {
        ToolEvent event = new ToolEvent();
        event.setToolId(tool.getId());
        event.setEventType(type);
        event.setEquipmentId(tool.getCurrentEquipmentId());
        event.setUsageQty(1L);
        event.setUsedTotalAfter(total);
        event.setOperator(session.getCurrentUserNameOrDefault());
        event.setOccurredAt(LocalDateTime.now(clock));
        events.save(event);
    }

    /**
     * 回减一次使用。低于上限时，保养状态回到使用中或可用。
     *
     * @param toolId 工具
     * @throws BusinessException 工具不存在，或使用量已经是 0
     */
    @Transactional
    public void reverse(Long toolId) {
        Tool tool = repository.findById(toolId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.TOOL_NOT_FOUND, "工具不存在"));
        long used = tool.getUsedCount() == null ? 0L : tool.getUsedCount();
        if (used < 1) {
            throw new BusinessException(MesBaseErrorCodes.TOOL_NOT_FOUND, "没有可回减的使用量");
        }
        long next = used - 1;
        tool.setUsedCount(next);
        if ("MAINTENANCE".equals(tool.getStatus()) && (tool.getLifeLimit() == null || next < tool.getLifeLimit())) {
            tool.setStatus(tool.getCurrentEquipmentId() == null ? "AVAILABLE" : "IN_USE");
        }
        repository.update(tool);
        ToolEvent event = new ToolEvent();
        event.setToolId(tool.getId());
        event.setEventType("USE_REVERSAL");
        event.setEquipmentId(tool.getCurrentEquipmentId());
        event.setUsageQty(1L);
        event.setUsedTotalAfter(next);
        event.setOperator(session.getCurrentUserNameOrDefault());
        event.setOccurredAt(LocalDateTime.now(clock));
        events.save(event);
    }
}
