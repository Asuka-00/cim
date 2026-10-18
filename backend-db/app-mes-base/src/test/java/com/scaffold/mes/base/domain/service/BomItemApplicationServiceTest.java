package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.BomItemSaveRequest;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.BomItemRepository;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 草稿才能改 BOM 行。行号不能重复。用量必须大于 0，序号 0 表示每次经过都消耗。
 */
class BomItemApplicationServiceTest {

    private BomItemRepository repository;
    private BomItemApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(BomItemRepository.class);
        BomVersionRepository versionRepository = mock(BomVersionRepository.class);
        MaterialRepository materialRepository = mock(MaterialRepository.class);
        UomRepository uomRepository = mock(UomRepository.class);
        StepRepository stepRepository = mock(StepRepository.class);
        service = new BomItemApplicationService(repository, versionRepository, materialRepository, uomRepository, stepRepository);
        BomVersion version = new BomVersion();
        version.setId(1L);
        version.setStatus("DRAFT");
        when(versionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(materialRepository.findById(4L)).thenReturn(Optional.of(new Material()));
        when(uomRepository.findById(8L)).thenReturn(Optional.of(new Uom()));
        when(stepRepository.findById(7L)).thenReturn(Optional.of(new Step()));
    }

    @Test
    void createDefaultsConsumeEveryPass() {
        service.create(1L, request(10, "1.5", null));

        ArgumentCaptor<BomItem> captor = ArgumentCaptor.forClass(BomItem.class);
        verify(repository).save(captor.capture());
        BomItem saved = captor.getValue();
        assertThat(saved.getConsumeMode()).isEqualTo("PER_WAFER");
        assertThat(saved.getRouteSeqNo()).isZero();
        assertThat(saved.getBackflush()).isEqualTo(1);
        assertThat(saved.getSubstitutePolicy()).isEqualTo("ALLOW");
        assertThat(saved.getQtyPer()).isEqualByComparingTo("1.5");
    }

    @Test
    void createRejectsDuplicateLine() {
        when(repository.findByLine(1L, 10)).thenReturn(Optional.of(new BomItem()));

        assertThatThrownBy(() -> service.create(1L, request(10, "1", "PER_DIE")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_ITEM_LINE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsNonPositiveQty() {
        assertThatThrownBy(() -> service.create(1L, request(20, "0", "PER_LOT")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_ITEM_QTY_INVALID);
    }

    @Test
    void createRejectsUnknownMode() {
        assertThatThrownBy(() -> service.create(1L, request(20, "1", "PER_HOUR")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_ITEM_CONSUME_MODE_INVALID);
    }

    @Test
    void createRejectsActiveVersion() {
        BomVersion active = new BomVersion();
        active.setId(1L);
        active.setStatus("ACTIVE");
        BomVersionRepository versions = mock(BomVersionRepository.class);
        when(versions.findById(1L)).thenReturn(Optional.of(active));
        BomItemApplicationService isolated = new BomItemApplicationService(repository, versions, mock(MaterialRepository.class),
                mock(UomRepository.class), mock(StepRepository.class));

        assertThatThrownBy(() -> isolated.create(1L, request(10, "1", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_VERSION_NOT_DRAFT);
    }

    private static BomItemSaveRequest request(int line, String qty, String mode) {
        BomItemSaveRequest request = new BomItemSaveRequest();
        request.setLineNo(line);
        request.setMaterialId(4L);
        request.setQtyPer(new BigDecimal(qty));
        request.setUomId(8L);
        request.setStepId(7L);
        request.setConsumeMode(mode);
        return request;
    }
}
