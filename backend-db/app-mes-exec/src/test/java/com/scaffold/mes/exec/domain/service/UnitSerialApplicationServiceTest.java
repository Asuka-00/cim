package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.UnitSerialRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.UnitSerial;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.UnitSerialRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ProductSerialPort;

/**
 * 只有产品打开序列号跟踪时才能登记。关闭时不写入。
 */
class UnitSerialApplicationServiceTest {

    private UnitSerialRepository serials;
    private ProductSerialPort productSerial;
    private UnitSerialApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lots = mock(LotRepository.class);
        serials = mock(UnitSerialRepository.class);
        productSerial = mock(ProductSerialPort.class);
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setProductId(1L);
        when(lots.findById(8L)).thenReturn(Optional.of(lot));
        when(serials.findByNo("U-1")).thenReturn(Optional.empty());
        service = new UnitSerialApplicationService(lots, serials, productSerial);
    }

    @Test
    void enabledProductSavesSerial() {
        when(productSerial.enabled(1L)).thenReturn(true);

        service.assign(8L, request());

        verify(serials).save(org.mockito.ArgumentMatchers.<UnitSerial>argThat(row ->
                "U-1".equals(row.getSerialNo()) && "NORMAL".equals(row.getStatus()) && row.getLotId().equals(8L)));
    }

    @Test
    void disabledProductRejected() {
        when(productSerial.enabled(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.assign(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UNIT_SERIAL_DISABLED);
        verify(serials, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static UnitSerialRequest request() {
        UnitSerialRequest request = new UnitSerialRequest();
        request.setSerialNo("U-1");
        return request;
    }
}
