package com.scaffold.mes.core.domain.service.shift;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.shift.ShiftCreateRequest;
import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.mes.core.domain.repository.shift.ShiftRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建班次。编码重复时拒绝。结束时间不晚于开始时间时记为跨日。
 */
class ShiftApplicationServiceTest {

    private ShiftRepository repository;
    private ShiftApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShiftRepository.class);
        when(repository.findByCode("DAY")).thenReturn(Optional.empty());
        service = new ShiftApplicationService(repository);
    }

    @Test
    void createsDayShift() {
        service.create(request("DAY", "白班", LocalTime.of(8, 0), LocalTime.of(16, 0)));

        verify(repository).save(org.mockito.ArgumentMatchers.<Shift>argThat(row ->
                "白班".equals(row.getShiftName()) && Integer.valueOf(0).equals(row.getCrossDay())));
    }

    @Test
    void nightShiftCrossesDay() {
        when(repository.findByCode("NIGHT")).thenReturn(Optional.empty());

        service.create(request("NIGHT", "夜班", LocalTime.of(22, 0), LocalTime.of(6, 0)));

        verify(repository).save(org.mockito.ArgumentMatchers.<Shift>argThat(row ->
                Integer.valueOf(1).equals(row.getCrossDay())));
    }

    @Test
    void duplicateCodeRejected() {
        when(repository.findByCode("DAY")).thenReturn(Optional.of(new Shift()));

        assertThatThrownBy(() -> service.create(request("DAY", "白班", LocalTime.of(8, 0), LocalTime.of(16, 0))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.SHIFT_INVALID);
    }

    private static ShiftCreateRequest request(String code, String name, LocalTime start, LocalTime end) {
        ShiftCreateRequest request = new ShiftCreateRequest();
        request.setShiftCode(code);
        request.setShiftName(name);
        request.setStartTime(start);
        request.setEndTime(end);
        return request;
    }
}
