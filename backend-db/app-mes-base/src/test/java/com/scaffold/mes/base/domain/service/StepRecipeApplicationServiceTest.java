package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepRecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.StepRecipeRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 限定产品的绑定优先于通用。同一站点、同一产品只留一个默认 Recipe。
 */
class StepRecipeApplicationServiceTest {

    private StepRecipeRepository repository;
    private StepRecipeApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepRecipeRepository.class);
        StepRepository stepRepository = mock(StepRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        service = new StepRecipeApplicationService(repository, stepRepository, recipeRepository, productRepository);
        when(stepRepository.findById(7L)).thenReturn(Optional.of(new Step()));
        when(recipeRepository.findById(4L)).thenReturn(Optional.of(new Recipe()));
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(new Recipe()));
        when(productRepository.findById(9L)).thenReturn(Optional.of(new Product()));
    }

    @Test
    void listReturnsEveryBinding() {
        StepRecipe generic = new StepRecipe();
        generic.setProductId(0L);
        when(repository.listByStep(7L)).thenReturn(List.of(generic));

        assertThat(service.list(7L)).containsExactly(generic);
    }

    @Test
    void listAllowedPrefersProductSpecific() {
        StepRecipe specific = row(1L, 9L, 4L);
        when(repository.listByStepAndProduct(7L, 9L)).thenReturn(List.of(specific));

        assertThat(service.listAllowed(7L, 9L)).containsExactly(specific);
    }

    @Test
    void listAllowedFallsBackToGeneric() {
        StepRecipe generic = row(2L, 0L, 5L);
        when(repository.listByStepAndProduct(7L, 9L)).thenReturn(List.of());
        when(repository.listByStepAndProduct(7L, 0L)).thenReturn(List.of(generic));

        assertThat(service.listAllowed(7L, 9L)).containsExactly(generic);
    }

    @Test
    void createClearsPreviousDefaultInSameScope() {
        StepRecipe previous = row(3L, 0L, 5L);
        previous.setIsDefault(1);
        when(repository.listByStepAndProduct(7L, 0L)).thenReturn(List.of(previous));

        StepRecipeSaveRequest request = request(null);
        request.setIsDefault(1);
        service.create(request);

        assertThat(previous.getIsDefault()).isZero();
        verify(repository).update(previous);
    }

    @Test
    void createRejectsDuplicate() {
        when(repository.findExact(7L, 4L, 0L)).thenReturn(Optional.of(new StepRecipe()));

        assertThatThrownBy(() -> service.create(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_RECIPE_EXISTS);
    }

    private static StepRecipeSaveRequest request(Long productId) {
        StepRecipeSaveRequest request = new StepRecipeSaveRequest();
        request.setStepId(7L);
        request.setRecipeId(4L);
        request.setProductId(productId);
        return request;
    }

    private static StepRecipe row(Long id, Long productId, Long recipeId) {
        StepRecipe binding = new StepRecipe();
        binding.setId(id);
        binding.setStepId(7L);
        binding.setProductId(productId);
        binding.setRecipeId(recipeId);
        binding.setIsDefault(0);
        return binding;
    }
}
