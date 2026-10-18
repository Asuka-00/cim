package com.scaffold.share.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class SqlSingleLineLogTest {

    private static final String MAPPER_NAME = "com.scaffold.system.infra.mapper.rbac.PermissionMapper";
    private static final String SQL_LOGGER_NAME = "SQL";

    private Logger mapperLogger;
    private Logger sqlLogger;
    private Level originalMapperLevel;
    private Level originalSqlLevel;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        mapperLogger = (Logger) LoggerFactory.getLogger(MAPPER_NAME);
        originalMapperLevel = mapperLogger.getLevel();
        mapperLogger.setLevel(Level.DEBUG);

        sqlLogger = (Logger) LoggerFactory.getLogger(SQL_LOGGER_NAME);
        originalSqlLevel = sqlLogger.getLevel();
        sqlLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        sqlLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        sqlLogger.detachAppender(listAppender);
        sqlLogger.setLevel(originalSqlLevel);
        mapperLogger.setLevel(originalMapperLevel);
        SqlSingleLineLog.setEnabled(true);
        MdcUtils.clear();
    }

    @Test
    void shouldFlushSqlExecOnUpdateResult() {
        MdcUtils.setTraceId("trace-001");
        MdcUtils.setTenantId("tenant-001");
        SqlSingleLineLog sqlLog = new SqlSingleLineLog(MAPPER_NAME);

        sqlLog.debug("==>  Preparing: UPDATE c_sys_permission SET permission_code = ? WHERE id = ?");
        sqlLog.debug("==> Parameters: APP_TENANT_MANAGEMENT(String), 1844674407370955215(Long)");
        sqlLog.debug("<==    Updates: 1");

        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        assertTrue(message.startsWith("SQL PermissionMapper "), "message should include mapper: " + message);
        assertTrue(message.contains("rows=1"), message);
        assertTrue(message.contains(
                "UPDATE c_sys_permission SET permission_code = 'APP_TENANT_MANAGEMENT' WHERE id = 1844674407370955215"),
                message);
        assertEquals(SQL_LOGGER_NAME, event.getLoggerName());
        assertStructuredArgument(event, "event", "SQL_EXEC");
        assertStructuredArgument(event, "mapper", MAPPER_NAME);
        assertStructuredArgument(event, "sql", "UPDATE c_sys_permission SET permission_code = ? WHERE id = ?");
        assertStructuredArgument(event, "params", "APP_TENANT_MANAGEMENT");
        assertStructuredArgument(event, "count", "1");
        assertStructuredArgument(event, "traceId", "trace-001");
        assertStructuredArgument(event, "tenantId", "tenant-001");
    }

    @Test
    void shouldFlushSqlExecOnSelectTotal() {
        SqlSingleLineLog sqlLog = new SqlSingleLineLog(MAPPER_NAME);

        sqlLog.debug("==>  Preparing: SELECT id FROM c_dispatch_event WHERE event_type = ?");
        sqlLog.debug("==> Parameters: NEW(String)");
        sqlLog.debug("<==      Total: 3");

        assertEquals(1, listAppender.list.size());
        String message = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("rows=3"), message);
        assertTrue(message.contains("SELECT id FROM c_dispatch_event WHERE event_type = 'NEW'"), message);
    }

    @Test
    void fillParametersShouldQuoteStringsAndKeepNumbers() {
        String filled = SqlSingleLineLog.fillParameters(
                "SELECT * FROM t WHERE code = ? AND id = ? AND flag = ?",
                List.of("A'B", "42", "null"));
        assertEquals("SELECT * FROM t WHERE code = 'A''B' AND id = 42 AND flag = null", filled);
    }

    private void assertStructuredArgument(ILoggingEvent event, String key, String value) {
        Object[] arguments = event.getArgumentArray();
        assertNotNull(arguments);
        assertTrue(Arrays.stream(arguments)
                .map(String::valueOf)
                .anyMatch(argument -> argument.contains(key) && argument.contains(value)));
    }
}
