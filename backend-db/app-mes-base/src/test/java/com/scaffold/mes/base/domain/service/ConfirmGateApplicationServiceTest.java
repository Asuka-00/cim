package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.repository.EsignRepository;

/**
 * 没有规则时直接通过。要求再次确认时，必须已有对应签名。
 */
class ConfirmGateApplicationServiceTest {

    private ConfirmRuleApplicationService rules;
    private EsignRepository esigns;
    private ConfirmGateApplicationService service;

    @BeforeEach
    void setUp() {
        rules = mock(ConfirmRuleApplicationService.class);
        esigns = mock(EsignRepository.class);
        service = new ConfirmGateApplicationService(rules, esigns);
    }

    @Test
    void noneAllowsWithoutSignature() {
        when(rules.required("SCRAP")).thenReturn("NONE");

        assertThat(service.allowed("SCRAP", "LOT", 8L)).isTrue();
    }

    @Test
    void reauthNeedsPrimarySignature() {
        when(rules.required("SCRAP")).thenReturn("REAUTH");
        when(esigns.exists("SCRAP", "LOT", 8L, "PRIMARY")).thenReturn(false);

        assertThat(service.allowed("SCRAP", "LOT", 8L)).isFalse();
    }
}
