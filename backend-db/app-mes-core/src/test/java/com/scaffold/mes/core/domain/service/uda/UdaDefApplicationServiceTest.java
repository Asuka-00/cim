package com.scaffold.mes.core.domain.service.uda;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.uda.UdaDefCreateRequest;
import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.repository.uda.UdaDefRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建自定义属性定义。同一对象类型下编码不能重复。
 */
class UdaDefApplicationServiceTest {

    private UdaDefRepository repository;
    private UdaDefApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UdaDefRepository.class);
        when(repository.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.empty());
        service = new UdaDefApplicationService(repository);
    }

    @Test
    void createsTextAttribute() {
        UdaDefCreateRequest request = request("LOT", "COLOR", "颜色");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<UdaDef>argThat(row ->
                "颜色".equals(row.getAttrName()) && "TEXT".equals(row.getDataType()) && Integer.valueOf(1).equals(row.getEnabled())));
    }

    @Test
    void duplicateCodeOnSameObjectRejected() {
        when(repository.findByObjectAndCode("LOT", "COLOR")).thenReturn(Optional.of(new UdaDef()));

        assertThatThrownBy(() -> service.create(request("LOT", "COLOR", "颜色")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.UDA_INVALID);
    }

    @Test
    void badObjectTypeRejected() {
        assertThatThrownBy(() -> service.create(request("TOOL", "COLOR", "颜色")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.UDA_INVALID);
    }

    @Test
    void badDataTypeRejected() {
        UdaDefCreateRequest request = request("LOT", "COLOR", "颜色");
        request.setDataType("FILE");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.UDA_INVALID);
    }

    @Test
    void enumStoresOptions() {
        UdaDefCreateRequest request = request("LOT", "COLOR", "颜色");
        request.setDataType("ENUM");
        request.setOptions(" red, blue ");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<UdaDef>argThat(row ->
                "[\"red\",\"blue\"]".equals(row.getOptionsJson())));
    }

    @Test
    void createStoresRequired() {
        UdaDefCreateRequest request = request("LOT", "COLOR", "颜色");
        request.setRequired(1);

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<UdaDef>argThat(row ->
                Integer.valueOf(1).equals(row.getRequired())));
    }

    @Test
    void enumWithoutOptionsRejected() {
        UdaDefCreateRequest request = request("LOT", "COLOR", "颜色");
        request.setDataType("ENUM");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.UDA_INVALID);
    }

    private static UdaDefCreateRequest request(String objectType, String code, String name) {
        UdaDefCreateRequest request = new UdaDefCreateRequest();
        request.setObjectType(objectType);
        request.setAttrCode(code);
        request.setAttrName(name);
        return request;
    }
}
