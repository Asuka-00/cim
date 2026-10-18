package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ConfirmRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.repository.ConfirmRuleRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 启用的规则返回确认方式。没有规则或已停用时返回 NONE。
 */
class ConfirmRuleApplicationServiceTest {

    private ConfirmRuleRepository repository;
    private ConfirmRuleApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConfirmRuleRepository.class);
        when(repository.findByAction("SCRAP")).thenReturn(Optional.empty());
        service = new ConfirmRuleApplicationService(repository);
    }

    @Test
    void enabledRuleRequiresReauth() {
        ConfirmRuleCreateRequest request = new ConfirmRuleCreateRequest();
        request.setActionCode("SCRAP");
        request.setConfirmMode("REAUTH");
        service.create(request);
        verify(repository).save(org.mockito.ArgumentMatchers.<ConfirmRule>argThat(row ->
                "REAUTH".equals(row.getConfirmMode()) && row.getEnabled().equals(1)));

        ConfirmRule saved = new ConfirmRule();
        saved.setConfirmMode("REAUTH");
        saved.setEnabled(1);
        when(repository.findByAction("SCRAP")).thenReturn(Optional.of(saved));
        assertThat(service.required("SCRAP")).isEqualTo("REAUTH");
    }

    @Test
    void disabledRuleIsNone() {
        ConfirmRule saved = new ConfirmRule();
        saved.setConfirmMode("SECOND_SIGN");
        saved.setEnabled(0);
        when(repository.findByAction("SCRAP")).thenReturn(Optional.of(saved));

        assertThat(service.required("SCRAP")).isEqualTo("NONE");
    }

    @Test
    void secondSignNeedsRole() {
        ConfirmRuleCreateRequest request = new ConfirmRuleCreateRequest();
        request.setActionCode("SCRAP");
        request.setConfirmMode("SECOND_SIGN");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.CONFIRM_RULE_INVALID);
    }
}
