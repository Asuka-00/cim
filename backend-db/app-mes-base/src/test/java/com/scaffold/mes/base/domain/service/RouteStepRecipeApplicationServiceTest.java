package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;

/**
 * 步骤级只要有记录，就整组替换站点级，不把两边拼在一起。
 */
class RouteStepRecipeApplicationServiceTest {

    private RouteStepRecipeRepository repository;
    private StepRecipeApplicationService stepRecipeService;
    private RouteStepRecipeApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteStepRecipeRepository.class);
        RouteStepRepository routeStepRepository = mock(RouteStepRepository.class);
        stepRecipeService = mock(StepRecipeApplicationService.class);
        service = new RouteStepRecipeApplicationService(repository, routeStepRepository, mock(RouteVersionRepository.class),
                mock(RecipeRepository.class), mock(ProductRepository.class), stepRecipeService);
        RouteStep step = new RouteStep();
        step.setId(3L);
        step.setStepId(7L);
        step.setRouteVersionId(1L);
        when(routeStepRepository.findById(3L)).thenReturn(Optional.of(step));
    }

    @Test
    void resolveUsesProductSpecificAndIgnoresStation() {
        RouteStepRecipe specific = override(8L, 9L);
        when(repository.listByStepAndProduct(3L, 9L)).thenReturn(List.of(specific));

        List<StepRecipe> resolved = service.resolve(3L, 9L);

        assertThat(resolved).extracting(StepRecipe::getRecipeId).containsExactly(8L);
        verify(stepRecipeService, never()).listAllowed(7L, 9L);
    }

    @Test
    void resolveUsesStepGenericBeforeStation() {
        RouteStepRecipe generic = override(5L, 0L);
        when(repository.listByStepAndProduct(3L, 9L)).thenReturn(List.of());
        when(repository.listByStepAndProduct(3L, 0L)).thenReturn(List.of(generic));

        List<StepRecipe> resolved = service.resolve(3L, 9L);

        assertThat(resolved).extracting(StepRecipe::getRecipeId).containsExactly(5L);
        verify(stepRecipeService, never()).listAllowed(7L, 9L);
    }

    @Test
    void listOwnedKeepsOnlyThisStep() {
        RouteStepRecipe mine = override(8L, 0L);
        RouteStepRecipe other = override(9L, 0L);
        other.setRouteStepId(4L);
        when(repository.listByVersion(1L)).thenReturn(List.of(mine, other));

        assertThat(service.listOwned(3L)).extracting(RouteStepRecipe::getRecipeId).containsExactly(8L);
    }

    @Test
    void resolveFallsBackToStationWhenStepHasNone() {
        StepRecipe station = new StepRecipe();
        station.setRecipeId(4L);
        when(repository.listByStepAndProduct(3L, 9L)).thenReturn(List.of());
        when(repository.listByStepAndProduct(3L, 0L)).thenReturn(List.of());
        when(stepRecipeService.listAllowed(7L, 9L)).thenReturn(List.of(station));

        assertThat(service.resolve(3L, 9L)).containsExactly(station);
    }

    private static RouteStepRecipe override(Long recipeId, Long productId) {
        RouteStepRecipe binding = new RouteStepRecipe();
        binding.setId(recipeId);
        binding.setRouteStepId(3L);
        binding.setRecipeId(recipeId);
        binding.setProductId(productId);
        binding.setIsDefault(0);
        return binding;
    }
}
