package com.scaffold.share.infra.repository.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.entity.SoftDeletableEntity;
import com.scaffold.share.id.AppIdGenerator;
import com.scaffold.share.id.AppIdGeneratorProperties;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

class DbRepositorySupportTest {

    @Test
    void batchSaveSkipsEmptyInput() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        support.batchSave(List.of());

        verify(mapper, never()).insertBatchSomeColumn(any());
    }

    @Test
    void queryPageNormalizesPageArguments() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        Page<TestEntity> result = new Page<>(1, 1);
        result.setRecords(List.of(entity(1L)));
        result.setTotal(1);
        when(mapper.selectPage(any(), any())).thenReturn(result);
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        PageDTO<TestEntity> page = support.queryPage(0, 0, support.wrapper());

        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(1);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void textHelpersHandleBlankValues() {
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mockMapper(), TestEntity.class);

        assertThat(support.isBlank(" ")).isTrue();
        assertThat(support.hasText("code")).isTrue();
    }

    @Test
    void wrapperUsesConcreteEntityClass() {
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mockMapper(), TestEntity.class);

        assertThat(support.wrapper().getEntityClass()).isEqualTo(TestEntity.class);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void findAllUsesConcreteEntityClassWrapper() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        when(mapper.selectList(any())).thenReturn(List.of());
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        support.findAll();

        ArgumentCaptor<LambdaQueryWrapper<TestEntity>> wrapperCaptor = ArgumentCaptor.forClass(
                (Class) LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getEntityClass()).isEqualTo(TestEntity.class);
    }

    @Test
    void batchSaveWithFillPopulatesMissingCommonFields() {
        AppIdGenerator.configure(idProperties());
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        TestEntity blank = new TestEntity();
        support.batchSaveWithFill(List.of(blank), session("T1", "U1"));

        assertThat(blank.getId()).isNotNull().isPositive();
        assertThat(blank.getTenantId()).isEqualTo("T1");
        assertThat(blank.getCreateBy()).isEqualTo("U1");
        assertThat(blank.getUpdateBy()).isEqualTo("U1");
        assertThat(blank.getCreateAt()).isNotNull();
        assertThat(blank.getUpdateAt()).isNotNull();
        verify(mapper).insertBatchSomeColumn(any());
    }

    @Test
    void batchSaveWithFillDoesNotOverwriteExistingFields() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        TestEntity preset = entity(99L);
        preset.setTenantId("OTHER");
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        preset.setCreateAt(created);
        support.batchSaveWithFill(List.of(preset), session("T1", "U1"));

        assertThat(preset.getId()).isEqualTo(99L);
        assertThat(preset.getTenantId()).isEqualTo("OTHER");
        assertThat(preset.getCreateAt()).isEqualTo(created);
        verify(mapper).insertBatchSomeColumn(any());
    }

    @Test
    void updateWithVersionThrowsWhenNoRowChanges() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        when(mapper.updateById(any(TestEntity.class))).thenReturn(0);
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        assertThatThrownBy(() -> support.updateWithVersion(entity(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(400101);
    }

    @Test
    void updateWithVersionAcceptsOneChangedRow() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        when(mapper.updateById(any(TestEntity.class))).thenReturn(1);
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        support.updateWithVersion(entity(1L));

        verify(mapper).updateById(any(TestEntity.class));
    }

    @Test
    void batchSaveWithFillSkipsEmptyInput() {
        BatchBaseMapper<TestEntity> mapper = mockMapper();
        DbRepositorySupport<TestEntity> support = new DbRepositorySupport<>(mapper, TestEntity.class);

        support.batchSaveWithFill(List.of(), session("T1", "U1"));

        verify(mapper, never()).insertBatchSomeColumn(any());
    }

    private static CurrentSessionProvider session(String tenantId, String userName) {
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentTenantIdOrDefault()).thenReturn(tenantId);
        when(session.getCurrentUserNameOrDefault()).thenReturn(userName);
        return session;
    }

    private static AppIdGeneratorProperties idProperties() {
        AppIdGeneratorProperties properties = new AppIdGeneratorProperties();
        properties.setWorkerId(0);
        properties.setBaseTime(1_767_196_800_000L);
        properties.setWorkerIdBitLength(6);
        properties.setSeqBitLength(6);
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static BatchBaseMapper<TestEntity> mockMapper() {
        return mock(BatchBaseMapper.class);
    }

    private static TestEntity entity(Long id) {
        TestEntity entity = new TestEntity();
        entity.setId(id);
        entity.setCreateAt(LocalDateTime.now());
        return entity;
    }

    private static final class TestEntity extends SoftDeletableEntity {
    }
}
