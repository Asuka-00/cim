package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.AreaSaveRequest;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 区域编码不能重复。上级不能指向自己或形成循环。被引用时不能删除。
 */
class AreaApplicationServiceTest {

    private AreaRepository repository;
    private AreaApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AreaRepository.class);
        service = new AreaApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("BAY-1")).thenReturn(Optional.of(new Area()));

        assertThatThrownBy(() -> service.create(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingParent() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(9L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_NOT_FOUND);
    }

    @Test
    void updateRejectsSelfParent() {
        Area current = area(1L, null);
        when(repository.findById(1L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(1L, request(1L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_PARENT_CYCLE);
    }

    @Test
    void updateRejectsCycle() {
        Area child = area(2L, null);
        Area parent = area(1L, 2L);
        when(repository.findById(2L)).thenReturn(Optional.of(child));
        when(repository.findById(1L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.update(2L, request(1L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_PARENT_CYCLE);
        verify(repository, never()).update(any());
    }

    @Test
    void deleteRejectsWhenReferenced() {
        when(repository.findById(1L)).thenReturn(Optional.of(area(1L, null)));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static AreaSaveRequest request(Long parentId) {
        AreaSaveRequest request = new AreaSaveRequest();
        request.setAreaCode("BAY-1");
        request.setAreaName("一号湾");
        request.setParentId(parentId);
        return request;
    }

    private static Area area(Long id, Long parentId) {
        Area area = new Area();
        area.setId(id);
        area.setParentId(parentId);
        return area;
    }
}
