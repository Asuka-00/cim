package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UserScopeCreateRequest;
import com.scaffold.mes.base.domain.repository.UserScopeRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 没有范围记录时不限制。有记录时只允许名单内的对象。
 */
class UserScopeApplicationServiceTest {

    private UserScopeRepository repository;
    private UserScopeApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserScopeRepository.class);
        service = new UserScopeApplicationService(repository);
    }

    @Test
    void emptyScopeAllowsAny() {
        when(repository.existsType("sam", "STEP")).thenReturn(false);

        assertThat(service.allowed("sam", "STEP", 6L)).isTrue();
    }

    @Test
    void listedStepOnly() {
        when(repository.existsType("sam", "STEP")).thenReturn(true);
        when(repository.exists("sam", "STEP", 5L)).thenReturn(true);
        when(repository.exists("sam", "STEP", 6L)).thenReturn(false);

        assertThat(service.allowed("sam", "STEP", 5L)).isTrue();
        assertThat(service.allowed("sam", "STEP", 6L)).isFalse();
    }

    @Test
    void duplicateRejected() {
        when(repository.exists("sam", "STEP", 5L)).thenReturn(true);
        UserScopeCreateRequest request = new UserScopeCreateRequest();
        request.setUserName("sam");
        request.setScopeType("STEP");
        request.setScopeId(5L);

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.USER_SCOPE_INVALID);
    }
}
