package com.scaffold.share.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.infra.mapper.audit.OperationLogMapper;

class OperationLogServiceTest {

    @Test
    void saveSafelyInsertsOperationLog() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        OperationLog operationLog = new OperationLog();
        OperationLogService service = new OperationLogService(mapper);

        service.saveSafely(operationLog);

        verify(mapper).insert(operationLog);
    }

    @Test
    void saveSafelyDoesNotThrowWhenInsertFails() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        OperationLog operationLog = new OperationLog();
        doThrow(new RuntimeException("db down")).when(mapper).insert(operationLog);
        OperationLogService service = new OperationLogService(mapper);

        assertDoesNotThrow(() -> service.saveSafely(operationLog));
    }

    @SuppressWarnings("unchecked")
    @Test
    void queryOperationLogPageMapsRecordsAndDelegatesPagination() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        OperationLog operationLog = new OperationLog();
        operationLog.setId(1L);
        operationLog.setTenantId("tenant");
        operationLog.setTraceId("trace-1");
        operationLog.setUserName("admin");
        operationLog.setOperation("查询操作日志");
        operationLog.setClassName("OperationLogControllerV1");
        operationLog.setMethodName("queryOperationLogList");
        operationLog.setRequestUri("/api/v1/audit/operation-logs");
        operationLog.setRequestMethod("GET");
        operationLog.setRequestIp("127.0.0.1");
        operationLog.setUserAgent("JUnit");
        operationLog.setRequestPayload("{}");
        operationLog.setResponsePayload("{}");
        operationLog.setSuccess(Boolean.TRUE);
        operationLog.setDurationMs(12L);
        operationLog.setErrorType(null);
        operationLog.setErrorMessage(null);
        operationLog.setCreateBy("admin");
        operationLog.setCreateAt(LocalDateTime.of(2026, 6, 8, 10, 0));

        PageDTO<OperationLog> page = new PageDTO<>();
        page.setRecords(List.of(operationLog));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(20);

        when(mapper.selectPage(any(PageDTO.class), any(Wrapper.class))).thenReturn(page);
        OperationLogService service = new OperationLogService(mapper);

        OperationLogQuery query = new OperationLogQuery();
        query.setTraceId("trace-1");
        query.setUserName("admin");
        query.setSuccess(Boolean.TRUE);
        query.setCreateAtStart(LocalDateTime.of(2026, 6, 8, 0, 0));
        query.setCreateAtEnd(LocalDateTime.of(2026, 6, 8, 23, 59, 59));

        PageDTO<OperationLogVO> result = service.queryOperationLogPage(1, 20, query);

        assertEquals(1, result.getTotal());
        assertEquals("trace-1", result.getRecords().get(0).getTraceId());
        assertEquals("查询操作日志", result.getRecords().get(0).getOperation());
        assertEquals(Boolean.TRUE, result.getRecords().get(0).getSuccess());
        verify(mapper).selectPage(any(PageDTO.class), any(Wrapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void queryOperationLogPageBuildsFilterWrapper() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        PageDTO<OperationLog> page = new PageDTO<>();
        page.setRecords(List.of());
        page.setTotal(0);
        when(mapper.selectPage(any(PageDTO.class), any(Wrapper.class))).thenReturn(page);
        OperationLogService service = new OperationLogService(mapper);

        OperationLogQuery query = new OperationLogQuery();
        query.setOperation("新增");
        query.setRequestMethod("POST");

        service.queryOperationLogPage(2, 10, query);

        ArgumentCaptor<PageDTO<OperationLog>> pageCaptor = ArgumentCaptor.forClass(PageDTO.class);
        ArgumentCaptor<Wrapper<OperationLog>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());

        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());
        Assertions.assertNotNull(wrapperCaptor.getValue());
    }
}
