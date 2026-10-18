package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Recipe 编码不能重复。适用设备类型有值时必须是设备类型。已有版本时不能删除。
 */
class RecipeApplicationServiceTest {

    private RecipeRepository repository;
    private RecipeApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RecipeRepository.class);
        service = new RecipeApplicationService(repository);
    }

    @Test
    void createAllowsBlankEquipmentType() {
        service.create(request(null));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEquipmentType()).isNull();
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void createRejectsUnknownEquipmentType() {
        assertThatThrownBy(() -> service.create(request("OVEN")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_TYPE_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("R-1")).thenReturn(Optional.of(new Recipe()));

        assertThatThrownBy(() -> service.create(request("PROCESS")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.RECIPE_CODE_EXISTS);
    }

    @Test
    void deleteRejectsWhenVersionExists() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(recipe));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.RECIPE_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static RecipeSaveRequest request(String equipmentType) {
        RecipeSaveRequest request = new RecipeSaveRequest();
        request.setRecipeCode("R-1");
        request.setRecipeName("曝光程序");
        request.setEquipmentType(equipmentType);
        return request;
    }
}
