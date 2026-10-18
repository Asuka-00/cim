package com.scaffold.mes.core.domain.service.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.notify.NotifyRuleCreateRequest;
import com.scaffold.mes.core.domain.dto.notify.NotifyRuleResponse;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.mes.core.domain.entity.notify.NotifyRule;
import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.mes.core.domain.repository.notify.NotifyRuleRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建通知规则。编码重复时拒绝。停用后按事件查不到它。
 */
class NotifyRuleApplicationServiceTest {

    private NotifyRuleRepository repository;
    private MesAlertRepository alerts;
    private NotifyRuleApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotifyRuleRepository.class);
        alerts = mock(MesAlertRepository.class);
        when(repository.findByCode("HOLD-TIMEOUT")).thenReturn(Optional.empty());
        when(alerts.findByEventAndRule(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        service = new NotifyRuleApplicationService(repository, alerts);
    }

    @Test
    void createsEnabledWarnRule() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setTitleTemplate("请处理 Hold");
        request.setContentTemplate("批已超时");
        request.setReceiverId("shift-lead");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<NotifyRule>argThat(row ->
                "Hold 超时".equals(row.getRuleName())
                        && "LOT_HOLD".equals(row.getEventType())
                        && "请处理 Hold".equals(row.getTitleTemplate())
                        && "批已超时".equals(row.getContentTemplate())
                        && "WARN".equals(row.getLevel())
                        && Integer.valueOf(1).equals(row.getEnabled())));
    }

    @Test
    void duplicateCodeRejected() {
        when(repository.findByCode("HOLD-TIMEOUT")).thenReturn(Optional.of(new NotifyRule()));
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setReceiverId("shift-lead");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void blankEventRejected() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType(" ");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void listsOnlyEnabledRulesForEvent() {
        NotifyRule enabled = new NotifyRule();
        enabled.setRuleCode("HOLD-TIMEOUT");
        enabled.setRuleName("Hold 超时");
        enabled.setEventType("LOT_HOLD");
        enabled.setLevel("WARN");
        enabled.setEnabled(1);
        NotifyRule disabled = new NotifyRule();
        disabled.setRuleCode("HOLD-OFF");
        disabled.setEventType("LOT_HOLD");
        disabled.setEnabled(0);
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(enabled, disabled));

        List<NotifyRuleResponse> rows = service.listEnabled("LOT_HOLD");

        assertThat(rows).extracting(NotifyRuleResponse::ruleCode).containsExactly("HOLD-TIMEOUT");
    }

    @Test
    void disabledRuleIsHiddenFromEventList() {
        NotifyRule rule = new NotifyRule();
        rule.setRuleCode("HOLD-TIMEOUT");
        rule.setEventType("LOT_HOLD");
        rule.setLevel("WARN");
        rule.setEnabled(1);
        when(repository.findById(7L)).thenReturn(Optional.of(rule));
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.disable(7L);

        assertThat(service.listEnabled("LOT_HOLD")).isEmpty();
    }

    @Test
    void disableRejectsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disable(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void raisesOneAlertForMatchedEvent() {
        NotifyRule rule = new NotifyRule();
        rule.setRuleCode("HOLD-TIMEOUT");
        rule.setRuleName("Hold 超时");
        rule.setEventType("LOT_HOLD");
        rule.setLevel("WARN");
        rule.setEnabled(1);
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                Long.valueOf(9L).equals(row.getEventId())
                        && "Hold 超时".equals(row.getTitle())
                        && "HOLD-TIMEOUT".equals(row.getRuleCode())
                        && "NEW".equals(row.getStatus())));
    }

    @Test
    void titleTemplateBecomesAlertTitle() {
        NotifyRule rule = rule("HOLD-TIMEOUT", "Hold 超时");
        rule.setTitleTemplate("请处理 Hold");
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "请处理 Hold".equals(row.getTitle())));
    }

    @Test
    void contentTemplateBecomesAlertContent() {
        NotifyRule rule = rule("HOLD-TIMEOUT", "Hold 超时");
        rule.setContentTemplate("批已超时");
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "批已超时".equals(row.getContent())));
    }

    @Test
    void createStoresErrorLevel() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setLevel("ERROR");
        request.setReceiverId("shift-lead");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<NotifyRule>argThat(row ->
                "ERROR".equals(row.getLevel())));
    }

    @Test
    void badLevelRejected() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setLevel("CRITICAL");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void alertUsesRuleLevel() {
        NotifyRule rule = rule("HOLD-TIMEOUT", "Hold 超时");
        rule.setLevel("ERROR");
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "ERROR".equals(row.getLevel())));
    }

    @Test
    void createStoresUserReceiver() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setReceiverType("USER");
        request.setReceiverId("zhang");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<NotifyRule>argThat(row ->
                "USER".equals(row.getReceiverType()) && "zhang".equals(row.getReceiverId())));
    }

    @Test
    void roleWithoutReceiverRejected() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setReceiverType("ROLE");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void badReceiverRejected() {
        NotifyRuleCreateRequest request = new NotifyRuleCreateRequest();
        request.setRuleCode("HOLD-TIMEOUT");
        request.setRuleName("Hold 超时");
        request.setEventType("LOT_HOLD");
        request.setReceiverType("TEAM");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.NOTIFY_RULE_INVALID);
    }

    @Test
    void alertUsesReceiverScope() {
        NotifyRule rule = rule("HOLD-TIMEOUT", "Hold 超时");
        rule.setReceiverType("USER");
        rule.setReceiverId("zhang");
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(rule));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "USER:zhang".equals(row.getReceiverScope())));
    }

    @Test
    void raisesOneAlertPerMatchedRule() {
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(
                rule("HOLD-TIMEOUT", "Hold 超时"),
                rule("HOLD-CRITICAL", "严重 Hold")));

        service.raiseForEvent(9L, "LOT_HOLD");

        ArgumentCaptor<MesAlert> saved = ArgumentCaptor.forClass(MesAlert.class);
        verify(alerts, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(MesAlert::getRuleCode)
                .containsExactly("HOLD-TIMEOUT", "HOLD-CRITICAL");
    }

    @Test
    void repeatedRuleDoesNotRaiseAgain() {
        when(repository.listEnabledByEvent("LOT_HOLD")).thenReturn(List.of(
                rule("HOLD-TIMEOUT", "Hold 超时"),
                rule("HOLD-CRITICAL", "严重 Hold")));
        when(alerts.findByEventAndRule(9L, "HOLD-TIMEOUT")).thenReturn(Optional.of(new MesAlert()));

        service.raiseForEvent(9L, "LOT_HOLD");

        verify(alerts).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "HOLD-CRITICAL".equals(row.getRuleCode())));
        verify(alerts, never()).save(org.mockito.ArgumentMatchers.<MesAlert>argThat(row ->
                "HOLD-TIMEOUT".equals(row.getRuleCode())));
    }

    private static NotifyRule rule(String code, String name) {
        NotifyRule rule = new NotifyRule();
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setEventType("LOT_HOLD");
        rule.setLevel("WARN");
        rule.setEnabled(1);
        return rule;
    }
}
