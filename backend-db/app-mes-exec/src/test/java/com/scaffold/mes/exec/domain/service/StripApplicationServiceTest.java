package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.StripConvertRequest;
import com.scaffold.mes.exec.domain.dto.StripCreateRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Strip;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.StripRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 建立 Strip 会把 Unit 数加进 Lot 副数量。编号重复或良品数超出时拒绝。
 */
class StripApplicationServiceTest {

    private StripRepository stripRepository;
    private Lot lot;
    private StripApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lots = mock(LotRepository.class);
        stripRepository = mock(StripRepository.class);
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setSubQty(10L);
        when(lots.findById(8L)).thenReturn(Optional.of(lot));
        when(stripRepository.findByCode("S-1")).thenReturn(Optional.empty());
        service = new StripApplicationService(lots, stripRepository);
    }

    @Test
    void createAddsSubQty() {
        StripCreateRequest request = request();
        request.setGoodQty(36);

        service.create(8L, request);

        assertThat(lot.getSubQty()).isEqualTo(50L);
        verify(stripRepository).save(org.mockito.ArgumentMatchers.<Strip>argThat(row ->
                "S-1".equals(row.getStripCode()) && "NORMAL".equals(row.getStatus()) && row.getUnitQty().equals(40)
                        && row.getGoodQty().equals(36) && row.getLotId().equals(8L)));
    }

    @Test
    void duplicateCodeRejected() {
        when(stripRepository.findByCode("S-1")).thenReturn(Optional.of(new Strip()));

        assertThatThrownBy(() -> service.create(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.STRIP_CODE_EXISTS);
        assertThat(lot.getSubQty()).isEqualTo(10L);
    }

    @Test
    void goodQtyAboveUnitRejected() {
        StripCreateRequest request = request();
        request.setGoodQty(41);

        assertThatThrownBy(() -> service.create(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.STRIP_QTY);
    }

    @Test
    void convertMarksStripsAndSwitchesUnit() {
        Strip first = strip("NORMAL", 36);
        Strip second = strip("NORMAL", 40);
        Strip done = strip("CONVERTED", 1);
        when(stripRepository.listByLot(8L)).thenReturn(List.of(first, second, done));
        StripConvertRequest request = new StripConvertRequest();
        request.setUnitId(9L);

        service.convert(8L, request);

        assertThat(first.getStatus()).isEqualTo("CONVERTED");
        assertThat(second.getStatus()).isEqualTo("CONVERTED");
        assertThat(lot.getQty()).isEqualTo(76L);
        assertThat(lot.getQtyUnitId()).isEqualTo(9L);
        verify(stripRepository).update(first);
        verify(stripRepository).update(second);
    }

    private static Strip strip(String status, int good) {
        Strip strip = new Strip();
        strip.setStatus(status);
        strip.setGoodQty(good);
        return strip;
    }

    private static StripCreateRequest request() {
        StripCreateRequest request = new StripCreateRequest();
        request.setStripCode("S-1");
        request.setUnitQty(40);
        return request;
    }
}
