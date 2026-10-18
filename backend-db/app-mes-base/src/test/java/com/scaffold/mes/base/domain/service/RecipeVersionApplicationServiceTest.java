package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RecipeVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.RecipeVersionRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * ACTIVE 不能改。复制出的草稿可以改，生效后旧版本失效，同时只留一个 ACTIVE。
 */
class RecipeVersionApplicationServiceTest {

    private final List<RecipeVersion> rows = new ArrayList<>();
    private RecipeVersionRepository repository;
    private RecipeRepository recipeRepository;
    private RecipeVersionApplicationService service;

    @BeforeEach
    void setUp() {
        rows.add(version(10L, 1, "ACTIVE", "{\"exp\":10}"));
        rows.add(version(11L, 2, "DRAFT", "{\"exp\":12}"));
        repository = mock(RecipeVersionRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        when(repository.findById(any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(repository.listActive(1L)).thenAnswer(invocation -> rows.stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .toList());
        when(repository.nextVersionNo(1L)).thenAnswer(invocation -> rows.stream()
                .map(RecipeVersion::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1);
        doAnswer(invocation -> {
            rows.add(invocation.getArgument(0));
            return null;
        }).when(repository).save(any());
        ApprovalRecordRepository approvalRepository = mock(ApprovalRecordRepository.class);
        ApprovalRecord submit = new ApprovalRecord();
        submit.setAssignee("sam");
        when(approvalRepository.findLatest("RECIPE_VERSION", 12L, "SUBMIT")).thenReturn(Optional.of(submit));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T03:00:00Z"), ZoneOffset.ofHours(8));
        service = new RecipeVersionApplicationService(repository, recipeRepository, mock(ApprovalRecordApplicationService.class),
                approvalRepository, openConfirms(), clock);
    }

    @Test
    void updateRejectsActiveVersion() {
        assertThatThrownBy(() -> service.update(1L, 10L, request("{\"exp\":11}")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.RECIPE_VERSION_LOCKED);
        assertThat(rows.get(0).getParamDesc()).isEqualTo("{\"exp\":10}");
    }

    @Test
    void copyThenEditThenActivateLeavesSingleActive() {
        service.copy(1L, 10L);
        RecipeVersion draft = rows.get(rows.size() - 1);
        draft.setId(12L);
        assertThat(draft.getVersionNo()).isEqualTo(3);
        assertThat(draft.getStatus()).isEqualTo("DRAFT");
        assertThat(draft.getParamDesc()).isEqualTo("{\"exp\":10}");

        service.update(1L, 12L, request("{\"exp\":11}"));
        assertThat(draft.getParamDesc()).isEqualTo("{\"exp\":11}");

        RouteVersionSubmitRequest submit = new RouteVersionSubmitRequest();
        submit.setAssignee("sam");
        service.submit(1L, 12L, "lee", submit);
        RouteVersionDecisionRequest decision = new RouteVersionDecisionRequest();
        decision.setOperator("sam");
        service.approve(1L, 12L, decision);

        assertThat(rows.stream().filter(row -> "ACTIVE".equals(row.getStatus()))).hasSize(1);
        assertThat(draft.getStatus()).isEqualTo("ACTIVE");
        assertThat(draft.getEffectiveAt()).isNotNull();
        assertThat(rows.get(0).getStatus()).isEqualTo("INACTIVE");
        assertThat(rows.get(0).getExpireAt()).isEqualTo(draft.getEffectiveAt());
        verify(recipeRepository, atLeastOnce()).lock(1L);
    }

    @Test
    void approveRejectsDraft() {
        assertThatThrownBy(() -> service.approve(1L, 11L, new RouteVersionDecisionRequest()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.RECIPE_VERSION_NOT_PENDING);
    }

    @Test
    void createStartsAtVersionOneWhenEmpty() {
        rows.clear();
        service.create(1L, request("{\"exp\":1}"));

        ArgumentCaptor<RecipeVersion> captor = ArgumentCaptor.forClass(RecipeVersion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
    }

    private static RecipeVersionSaveRequest request(String paramDesc) {
        RecipeVersionSaveRequest request = new RecipeVersionSaveRequest();
        request.setParamDesc(paramDesc);
        return request;
    }

    private static RecipeVersion version(Long id, int versionNo, String status, String paramDesc) {
        RecipeVersion version = new RecipeVersion();
        version.setId(id);
        version.setRecipeId(1L);
        version.setVersionNo(versionNo);
        version.setStatus(status);
        version.setParamDesc(paramDesc);
        return version;
    }

    private static ConfirmGateApplicationService openConfirms() {
        ConfirmGateApplicationService confirms = mock(ConfirmGateApplicationService.class);
        when(confirms.allowed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        return confirms;
    }
}
