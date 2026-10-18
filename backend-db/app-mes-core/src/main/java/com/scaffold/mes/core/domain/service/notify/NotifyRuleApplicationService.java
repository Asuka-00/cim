package com.scaffold.mes.core.domain.service.notify;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.notify.NotifyRuleCreateRequest;
import com.scaffold.mes.core.domain.dto.notify.NotifyRuleResponse;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.mes.core.domain.entity.notify.NotifyRule;
import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.mes.core.domain.repository.notify.NotifyRuleRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * NotifyRuleApplicationService 通知规则
 * <p>
 * 编码不能重复。新建时启用。每条已启用规则各生成一条预警，同一事件和同一规则不重复生成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class NotifyRuleApplicationService {

    private static final Set<String> LEVELS = Set.of("INFO", "WARN", "ERROR");

    private static final Set<String> RECEIVERS = Set.of("ROLE", "USER", "AREA", "ALL");

    private final NotifyRuleRepository repository;

    private final MesAlertRepository alerts;

    /**
     * 新建规则。
     *
     * @param request 编码、名称和事件类型
     * @throws BusinessException 编码已存在，名称或事件类型为空，或级别不合法
     */
    @Transactional
    public void create(NotifyRuleCreateRequest request) {
        if (request.getRuleName() == null || request.getRuleName().isBlank()
                || request.getEventType() == null || request.getEventType().isBlank()) {
            throw new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "规则名称和事件类型不能为空");
        }
        String level = request.getLevel();
        if (level == null || level.isBlank()) {
            level = "WARN";
        } else if (!LEVELS.contains(level)) {
            throw new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "预警级别只能是 INFO、WARN 或 ERROR");
        }
        String receiver = request.getReceiverType();
        if (receiver == null || receiver.isBlank()) {
            receiver = "ROLE";
        } else if (!RECEIVERS.contains(receiver)) {
            throw new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "接收范围只能是 ROLE、USER、AREA 或 ALL");
        }
        String receiverId = request.getReceiverId();
        if (receiverId != null && receiverId.isBlank()) {
            receiverId = null;
        }
        if (!"ALL".equals(receiver) && receiverId == null) {
            throw new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "接收范围不是 ALL 时必须填写接收对象");
        }
        if ("ALL".equals(receiver)) {
            receiverId = null;
        }
        if (repository.findByCode(request.getRuleCode()).isPresent()) {
            throw new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "规则编码已存在");
        }
        NotifyRule rule = new NotifyRule();
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setEventType(request.getEventType());
        rule.setTitleTemplate(request.getTitleTemplate());
        rule.setContentTemplate(request.getContentTemplate());
        rule.setLevel(level);
        rule.setReceiverType(receiver);
        rule.setReceiverId(receiverId);
        rule.setEnabled(1);
        repository.save(rule);
    }

    /**
     * 列出某个事件上已启用的规则。
     *
     * @param eventType 事件类型
     * @return 已启用的规则。事件类型为空时为空列表
     */
    public List<NotifyRuleResponse> listEnabled(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return List.of();
        }
        return repository.listEnabledByEvent(eventType).stream()
                .filter(rule -> Integer.valueOf(1).equals(rule.getEnabled()))
                .map(rule -> new NotifyRuleResponse(rule.getId(), rule.getRuleCode(), rule.getRuleName(), rule.getEventType(),
                        rule.getLevel(), rule.getTitleTemplate(), rule.getContentTemplate(), rule.getReceiverType(),
                        rule.getReceiverId()))
                .toList();
    }

    /**
     * 停用规则。
     *
     * @param id 规则
     * @throws BusinessException 规则不存在
     */
    @Transactional
    public void disable(Long id) {
        NotifyRule rule = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.NOTIFY_RULE_INVALID, "通知规则不存在"));
        rule.setEnabled(0);
        repository.update(rule);
    }

    /**
     * 每条已启用规则各生成一条预警，并记下规则编码。同一事件和同一规则不重复生成。
     *
     * @param eventId 领域事件
     * @param eventType 事件类型
     */
    @Transactional
    public void raiseForEvent(Long eventId, String eventType) {
        if (eventId == null) {
            return;
        }
        for (NotifyRuleResponse rule : listEnabled(eventType)) {
            if (alerts.findByEventAndRule(eventId, rule.ruleCode()).isPresent()) {
                continue;
            }
            MesAlert alert = new MesAlert();
            alert.setAlertType("SYSTEM");
            alert.setLevel(rule.level());
            alert.setTitle(titleOf(rule));
            alert.setContent(contentOf(rule));
            alert.setRuleCode(rule.ruleCode());
            alert.setStatus("NEW");
            alert.setReceiverScope(scopeOf(rule));
            alert.setEventId(eventId);
            alerts.save(alert);
        }
    }

    private static String scopeOf(NotifyRuleResponse rule) {
        String type = rule.receiverType() == null || rule.receiverType().isBlank() ? "ROLE" : rule.receiverType();
        if ("ALL".equals(type) || rule.receiverId() == null || rule.receiverId().isBlank()) {
            return type;
        }
        return type + ":" + rule.receiverId();
    }

    private static String titleOf(NotifyRuleResponse rule) {
        if (rule.titleTemplate() == null || rule.titleTemplate().isBlank()) {
            return rule.ruleName();
        }
        return rule.titleTemplate();
    }

    private static String contentOf(NotifyRuleResponse rule) {
        if (rule.contentTemplate() == null || rule.contentTemplate().isBlank()) {
            return null;
        }
        return rule.contentTemplate();
    }
}
