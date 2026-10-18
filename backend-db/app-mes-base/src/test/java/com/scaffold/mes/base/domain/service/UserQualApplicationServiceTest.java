package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.UserQual;
import com.scaffold.mes.base.domain.repository.UserQualRepository;

/**
 * 没有资质记录时允许操作。生效止已过则不允许，扫描后标成 EXPIRED。
 */
class UserQualApplicationServiceTest {

    private UserQualRepository repository;
    private UserQualApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserQualRepository.class);
        service = new UserQualApplicationService(repository);
    }

    @Test
    void missingRecordIsAllowed() {
        when(repository.find("sam", "EQUIPMENT", 3L)).thenReturn(Optional.empty());

        assertThat(service.allowed("sam", "EQUIPMENT", 3L, LocalDateTime.of(2026, 10, 16, 9, 0))).isTrue();
    }

    @Test
    void pastValidToIsNotAllowedAndExpires() {
        UserQual qual = new UserQual();
        qual.setStatus("VALID");
        qual.setValidTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        LocalDateTime now = LocalDateTime.of(2026, 10, 16, 9, 0);
        when(repository.find("sam", "EQUIPMENT", 3L)).thenReturn(Optional.of(qual));
        when(repository.listDue(now)).thenReturn(List.of(qual));

        assertThat(service.allowed("sam", "EQUIPMENT", 3L, now)).isFalse();
        assertThat(service.expireDue(now)).isEqualTo(1);
        assertThat(qual.getStatus()).isEqualTo("EXPIRED");
        verify(repository).update(qual);
    }
}
