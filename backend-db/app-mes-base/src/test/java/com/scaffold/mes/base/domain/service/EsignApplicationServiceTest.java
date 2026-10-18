package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EsignRequest;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.entity.Esign;
import com.scaffold.mes.base.domain.repository.ConfirmRuleRepository;
import com.scaffold.mes.base.domain.repository.EsignRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 需要再次确认时由当前用户签名。不需要确认时不写记录。
 */
class EsignApplicationServiceTest {

    private ConfirmRuleRepository rules;
    private EsignRepository repository;
    private EsignApplicationService service;

    @BeforeEach
    void setUp() {
        rules = mock(ConfirmRuleRepository.class);
        repository = mock(EsignRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T23:00:00Z"), ZoneOffset.ofHours(8));
        service = new EsignApplicationService(rules, repository, session, clock);
    }

    @Test
    void reauthSignsCurrentUser() {
        when(rules.findByAction("SCRAP")).thenReturn(Optional.of(rule("REAUTH", null)));

        service.sign(request(null));

        verify(repository).save(org.mockito.ArgumentMatchers.<Esign>argThat(row ->
                "sam".equals(row.getSigner()) && "PRIMARY".equals(row.getSignType()) && row.getBizId().equals(8L)));
    }

    @Test
    void noneDoesNotSign() {
        when(rules.findByAction("SCRAP")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sign(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ESIGN_NOT_REQUIRED);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void secondSignerMustDiffer() {
        when(rules.findByAction("SCRAP")).thenReturn(Optional.of(rule("SECOND_SIGN", "QA")));

        assertThatThrownBy(() -> service.sign(request("sam")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ESIGN_SIGNER);
    }

    private static ConfirmRule rule(String mode, String role) {
        ConfirmRule rule = new ConfirmRule();
        rule.setConfirmMode(mode);
        rule.setEnabled(1);
        rule.setSecondRoleCode(role);
        return rule;
    }

    private static EsignRequest request(String signer) {
        EsignRequest request = new EsignRequest();
        request.setActionCode("SCRAP");
        request.setBizType("LOT");
        request.setBizId(8L);
        request.setSigner(signer);
        return request;
    }
}
