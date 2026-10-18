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

import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.repository.uda.UdaDefRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotUdaPutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 给 Lot 写入已定义的自定义属性，并读出已写入的值。DATE 必须是 yyyy-MM-dd。
 */
class LotUdaApplicationServiceTest {

    private LotRepository lots;
    private UdaDefRepository definitions;
    private LotUdaApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        definitions = mock(UdaDefRepository.class);
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(definitions.listRequired("LOT")).thenReturn(List.of());
        service = new LotUdaApplicationService(lots, definitions);
    }

    @Test
    void writesValue() {
        Lot lot = new Lot();
        when(lots.findById(7L)).thenReturn(Optional.of(lot));

        service.put(7L, request("COLOR", "red"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"COLOR\":\"red\"}".equals(row.getExtJson())));
    }

    @Test
    void mergesExistingValue() {
        Lot lot = new Lot();
        lot.setExtJson("{\"SIZE\":\"1\"}");
        when(lots.findById(7L)).thenReturn(Optional.of(lot));

        service.put(7L, request("COLOR", "red"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"SIZE\":\"1\",\"COLOR\":\"red\"}".equals(row.getExtJson())));
    }

    @Test
    void undefinedRejected() {
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.empty());
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "red")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    @Test
    void disabledRejected() {
        UdaDef def = new UdaDef();
        def.setEnabled(0);
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "red")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    @Test
    void showsWrittenValues() {
        Lot lot = new Lot();
        lot.setExtJson("{\"COLOR\":\"red\"}");
        when(lots.findById(7L)).thenReturn(Optional.of(lot));

        assertThat(service.show(7L)).containsEntry("COLOR", "red");
    }

    @Test
    void showEmptyWhenUnset() {
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThat(service.show(7L)).isEmpty();
    }

    @Test
    void listsMissingRequiredCodes() {
        UdaDef color = new UdaDef();
        color.setAttrCode("COLOR");
        UdaDef size = new UdaDef();
        size.setAttrCode("SIZE");
        when(definitions.listRequired("LOT")).thenReturn(List.of(color, size));
        Lot lot = new Lot();
        lot.setExtJson("{\"COLOR\":\"red\"}");
        when(lots.findById(7L)).thenReturn(Optional.of(lot));

        assertThat(service.missing(7L)).containsExactly("SIZE");
    }

    @Test
    void numberValueAccepted() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("NUMBER");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        service.put(7L, request("COLOR", "-12.5"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"COLOR\":\"-12.5\"}".equals(row.getExtJson())));
    }

    @Test
    void numberValueRejected() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("NUMBER");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "red")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    @Test
    void boolValueAccepted() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("BOOL");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        service.put(7L, request("COLOR", "true"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"COLOR\":\"true\"}".equals(row.getExtJson())));
    }

    @Test
    void boolValueRejected() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("BOOL");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "yes")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    @Test
    void dateValueAccepted() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("DATE");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        service.put(7L, request("COLOR", "2026-10-16"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"COLOR\":\"2026-10-16\"}".equals(row.getExtJson())));
    }

    @Test
    void dateValueRejected() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("DATE");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "2026-02-31")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    @Test
    void enumValueAccepted() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("ENUM");
        def.setOptionsJson("[\"red\",\"blue\"]");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        service.put(7L, request("COLOR", "red"));

        verify(lots).update(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                "{\"COLOR\":\"red\"}".equals(row.getExtJson())));
    }

    @Test
    void enumValueRejected() {
        UdaDef def = new UdaDef();
        def.setEnabled(1);
        def.setDataType("ENUM");
        def.setOptionsJson("[\"red\",\"blue\"]");
        when(definitions.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(def));
        when(lots.findById(7L)).thenReturn(Optional.of(new Lot()));

        assertThatThrownBy(() -> service.put(7L, request("COLOR", "green")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.UDA_VALUE);
    }

    private static LotUdaPutRequest request(String code, String value) {
        LotUdaPutRequest request = new LotUdaPutRequest();
        request.setAttrCode(code);
        request.setValue(value);
        return request;
    }
}
