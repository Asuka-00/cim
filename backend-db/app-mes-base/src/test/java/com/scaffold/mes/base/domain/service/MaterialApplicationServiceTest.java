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
import com.scaffold.mes.base.domain.dto.MaterialSaveRequest;
import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 物料默认是 RAW，基础计量单位必须存在。被换算引用时不能删除。
 */
class MaterialApplicationServiceTest {

    private MaterialRepository repository;
    private UomRepository uomRepository;
    private MaterialApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(MaterialRepository.class);
        uomRepository = mock(UomRepository.class);
        service = new MaterialApplicationService(repository, uomRepository);
        when(uomRepository.findById(8L)).thenReturn(Optional.of(new Uom()));
    }

    @Test
    void createDefaultsToRawAndNonCritical() {
        service.create(request());

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(repository).save(captor.capture());
        Material saved = captor.getValue();
        assertThat(saved.getMaterialType()).isEqualTo("RAW");
        assertThat(saved.getIsCritical()).isZero();
        assertThat(saved.getNeedIncomingInspect()).isZero();
        assertThat(saved.getBaseUomId()).isEqualTo(8L);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("RESIN")).thenReturn(Optional.of(new Material()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.MATERIAL_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownType() {
        MaterialSaveRequest request = request();
        request.setMaterialType("INK");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.MATERIAL_TYPE_INVALID);
    }

    @Test
    void createRejectsMissingUom() {
        when(uomRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_NOT_FOUND);
    }

    @Test
    void deleteRejectsWhenReferenced() {
        Material material = new Material();
        material.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(material));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.MATERIAL_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static MaterialSaveRequest request() {
        MaterialSaveRequest request = new MaterialSaveRequest();
        request.setMaterialCode("RESIN");
        request.setMaterialName("树脂");
        request.setBaseUomId(8L);
        return request;
    }
}
