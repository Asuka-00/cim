package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.print.PrintLog;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.repository.print.PrintLogRepository;
import com.scaffold.mes.core.domain.repository.print.PrintTemplateRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotPrintRequest;
import com.scaffold.mes.exec.domain.dto.LotPrintResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 用已启用的模板记一条 Lot 打印。同一 Lot 和同一模板再次打印记成重打。可以列出记录。
 */
class LotPrintApplicationServiceTest {

    private LotRepository lots;
    private PrintTemplateRepository templates;
    private PrintLogRepository logs;
    private LotPrintApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        templates = mock(PrintTemplateRepository.class);
        logs = mock(PrintLogRepository.class);
        Lot lot = new Lot();
        lot.setId(7L);
        lot.setLotNo("L-1");
        when(lots.findById(7L)).thenReturn(Optional.of(lot));
        PrintTemplate template = new PrintTemplate();
        template.setId(3L);
        template.setTplType("LOT_TRAVELER");
        template.setEnabled(1);
        when(templates.findByCode("LOT-1")).thenReturn(Optional.of(template));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotPrintApplicationService(lots, templates, logs, clock);
    }

    @Test
    void recordsFirstPrint() {
        LotPrintRequest request = new LotPrintRequest();
        request.setTplCode("LOT-1");
        request.setCopies(2);

        service.record(7L, request);

        verify(logs).save(org.mockito.ArgumentMatchers.<PrintLog>argThat(row ->
                Long.valueOf(3L).equals(row.getTplId())
                        && "LOT".equals(row.getBizType())
                        && "L-1".equals(row.getBizCode())
                        && Integer.valueOf(2).equals(row.getCopies())
                        && Integer.valueOf(0).equals(row.getIsReprint())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getPrintedAt())));
    }

    @Test
    void secondPrintIsReprint() {
        when(logs.exists("LOT", 7L, 3L)).thenReturn(true);
        LotPrintRequest request = new LotPrintRequest();
        request.setTplCode("LOT-1");

        service.record(7L, request);

        verify(logs).save(org.mockito.ArgumentMatchers.<PrintLog>argThat(row ->
                Integer.valueOf(1).equals(row.getIsReprint())));
    }

    @Test
    void disabledTemplateRejected() {
        PrintTemplate template = new PrintTemplate();
        template.setEnabled(0);
        when(templates.findByCode("LOT-1")).thenReturn(Optional.of(template));
        LotPrintRequest request = new LotPrintRequest();
        request.setTplCode("LOT-1");

        assertThatThrownBy(() -> service.record(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.PRINT_TEMPLATE_INVALID);
    }

    @Test
    void zeroCopiesRejected() {
        LotPrintRequest request = new LotPrintRequest();
        request.setTplCode("LOT-1");
        request.setCopies(0);

        assertThatThrownBy(() -> service.record(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.PRINT_COPIES);
    }

    @Test
    void listsPrintsInTimeOrder() {
        PrintLog first = new PrintLog();
        first.setTplId(3L);
        first.setCopies(1);
        first.setIsReprint(0);
        first.setPrintedAt(LocalDateTime.parse("2026-10-16T16:00"));
        when(logs.listByBiz("LOT", 7L)).thenReturn(List.of(first));

        List<LotPrintResponse> rows = service.list(7L);

        assertThat(rows).containsExactly(new LotPrintResponse(3L, 1, 0, LocalDateTime.parse("2026-10-16T16:00")));
    }
}
