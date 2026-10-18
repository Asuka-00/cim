package com.scaffold.system.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.infra.mapper.db.rbac.UserMapper;
import com.scaffold.system.infra.repository.db.rbac.UserDbRepositoryImpl;

class UserDbRepositoryImplTest {

    @Test
    void findByUserNameReturnsEmptyForBlankValue() {
        UserMapper mapper = mock(UserMapper.class);
        UserDbRepositoryImpl repository = new UserDbRepositoryImpl(
                mapper, mock(UserTenantRepository.class), mock(CurrentSessionProvider.class));

        assertThat(repository.findByUserName("")).isEmpty();
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void selectByUserNameWithTenantDelegatesToFindByUserName() {
        UserMapper mapper = mock(UserMapper.class);
        UserDbRepositoryImpl repository = new UserDbRepositoryImpl(
                mapper, mock(UserTenantRepository.class), mock(CurrentSessionProvider.class));

        assertThat(repository.selectByUserNameWithTenant("", "t001")).isEmpty();
        verify(mapper, never()).selectByUserNameWithTenant(any(), any());
    }
}
