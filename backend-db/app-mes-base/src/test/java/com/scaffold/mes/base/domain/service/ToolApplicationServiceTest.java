package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ToolCheckoutRequest;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.entity.ToolEvent;
import com.scaffold.mes.base.domain.repository.ToolEventRepository;
import com.scaffold.mes.base.domain.repository.ToolRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 可用工具领用后变为 IN_USE，并记下设备。使用中的工具不能再领。
 */
class ToolApplicationServiceTest {

    private ToolRepository repository;
    private ToolEventRepository events;
    private Tool tool;
    private ToolApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ToolRepository.class);
        tool = new Tool();
        tool.setId(5L);
        tool.setStatus("AVAILABLE");
        tool.setEnabled(1);
        when(repository.findById(5L)).thenReturn(Optional.of(tool));
        ToolEventRepository events = mock(ToolEventRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        this.events = events;
        service = new ToolApplicationService(repository, events, session, clock);
    }

    @Test
    void checkoutMountsOnEquipment() {
        ToolCheckoutRequest request = new ToolCheckoutRequest();
        request.setEquipmentId(3L);

        service.checkout(5L, request);

        assertThat(tool.getStatus()).isEqualTo("IN_USE");
        assertThat(tool.getCurrentEquipmentId()).isEqualTo(3L);
        verify(repository).update(tool);
    }

    @Test
    void inUseCannotCheckout() {
        tool.setStatus("IN_USE");
        ToolCheckoutRequest request = new ToolCheckoutRequest();
        request.setEquipmentId(9L);

        assertThatThrownBy(() -> service.checkout(5L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.TOOL_NOT_AVAILABLE);
        verify(repository, never()).update(tool);
    }

    @Test
    void returnClearsEquipment() {
        tool.setStatus("IN_USE");
        tool.setCurrentEquipmentId(3L);

        service.returnTool(5L);

        assertThat(tool.getStatus()).isEqualTo("AVAILABLE");
        assertThat(tool.getCurrentEquipmentId()).isNull();
    }

    @Test
    void useReachesLimitAndSetsMaintenance() {
        tool.setUsedCount(99L);
        tool.setLifeLimit(100L);
        tool.setStatus("IN_USE");

        service.use(5L);

        assertThat(tool.getUsedCount()).isEqualTo(100L);
        assertThat(tool.getStatus()).isEqualTo("MAINTENANCE");
        verify(repository).update(tool);
        verify(events).save(org.mockito.ArgumentMatchers.<ToolEvent>argThat(row ->
                "USE".equals(row.getEventType()) && row.getUsageQty().equals(1L) && row.getUsedTotalAfter().equals(100L)
                        && "sam".equals(row.getOperator())));
    }

    @Test
    void reverseWritesReversalAndLeavesMaintenance() {
        tool.setUsedCount(100L);
        tool.setLifeLimit(100L);
        tool.setStatus("MAINTENANCE");
        tool.setCurrentEquipmentId(3L);

        service.reverse(5L);

        assertThat(tool.getUsedCount()).isEqualTo(99L);
        assertThat(tool.getStatus()).isEqualTo("IN_USE");
        verify(events).save(org.mockito.ArgumentMatchers.<ToolEvent>argThat(row ->
                "USE_REVERSAL".equals(row.getEventType()) && row.getUsedTotalAfter().equals(99L)));
    }

    @Test
    void crossingNinetyWritesWarning() {
        tool.setUsedCount(89L);
        tool.setLifeLimit(100L);
        tool.setStatus("IN_USE");

        service.use(5L);

        verify(events, times(2)).save(org.mockito.ArgumentMatchers.<ToolEvent>argThat(row ->
                row.getUsedTotalAfter().equals(90L)));
        verify(events).save(org.mockito.ArgumentMatchers.<ToolEvent>argThat(row -> "LIFE_WARN".equals(row.getEventType())));
    }

    @Test
    void usedUpToolIsNotWithinLife() {
        tool.setUsedCount(100L);
        tool.setLifeLimit(100L);

        assertThat(service.withinLife(5L)).isFalse();
    }

    @Test
    void availableCannotReturn() {
        assertThatThrownBy(() -> service.returnTool(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.TOOL_NOT_IN_USE);
        verify(repository, never()).update(tool);
    }
}
