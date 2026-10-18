package com.scaffold.share.audit;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.infra.mapper.audit.OperationLogMapper;
import com.scaffold.share.logging.MdcUtils;
import com.scaffold.share.utils.PageQueryUtils;

/**
 * OperationLogService 操作日志直写数据库服务
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper operationLogMapper;
    private final AsyncTaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public OperationLogService(OperationLogMapper operationLogMapper,
            @Qualifier("virtualThreadTaskExecutor") ObjectProvider<AsyncTaskExecutor> taskExecutorProvider,
            ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {
        this(operationLogMapper,
                taskExecutorProvider.getIfAvailable(OperationLogService::syncTaskExecutor),
                transactionManagerProvider.getIfAvailable());
    }

    OperationLogService(OperationLogMapper operationLogMapper) {
        this(operationLogMapper, syncTaskExecutor(), null);
    }

    OperationLogService(OperationLogMapper operationLogMapper, AsyncTaskExecutor taskExecutor,
            PlatformTransactionManager transactionManager) {
        this.operationLogMapper = operationLogMapper;
        this.taskExecutor = taskExecutor;
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (this.transactionTemplate != null) {
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    private static AsyncTaskExecutor syncTaskExecutor() {
        return new TaskExecutorAdapter(Runnable::run);
    }

    /**
     * 保存操作日志。日志写入失败不影响业务流程。
     *
     * @param operationLog 操作日志
     */
    public void saveSafely(OperationLog operationLog) {
        Map<String, String> mdcContext = MdcUtils.captureContext();
        taskExecutor.execute(() -> MdcUtils.runWithContext(mdcContext, () -> insertSafely(operationLog)));
    }

    private void insertSafely(OperationLog operationLog) {
        long startNanos = System.nanoTime();
        try {
            if (transactionTemplate == null) {
                operationLogMapper.insert(operationLog);
            } else {
                transactionTemplate.executeWithoutResult(status -> operationLogMapper.insert(operationLog));
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to insert operation log, traceId={}, operation={}",
                    operationLog == null ? null : operationLog.getTraceId(),
                    operationLog == null ? null : operationLog.getOperation(),
                    ex);
        } finally {
            log.info("Operation log insert stage: traceId={}, operation={}, durationMs={}",
                    operationLog == null ? MdcUtils.getOrCreateTraceId() : operationLog.getTraceId(),
                    operationLog == null ? null : operationLog.getOperation(),
                    Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
        }
    }

    /**
     * 分页查询操作日志。
     *
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param query    查询条件
     * @return 分页结果
     */
    public PageDTO<OperationLogVO> queryOperationLogPage(int page, int pageSize, OperationLogQuery query) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(hasText(query.getTraceId()), OperationLog::getTraceId, query.getTraceId())
                    .like(hasText(query.getUserName()), OperationLog::getUserName, query.getUserName())
                    .like(hasText(query.getOperation()), OperationLog::getOperation, query.getOperation())
                    .like(hasText(query.getClassName()), OperationLog::getClassName, query.getClassName())
                    .like(hasText(query.getMethodName()), OperationLog::getMethodName, query.getMethodName())
                    .like(hasText(query.getRequestUri()), OperationLog::getRequestUri, query.getRequestUri())
                    .eq(hasText(query.getRequestMethod()), OperationLog::getRequestMethod, query.getRequestMethod())
                    .eq(query.getSuccess() != null, OperationLog::getSuccess, query.getSuccess())
                    .ge(query.getCreateAtStart() != null, OperationLog::getCreateAt, query.getCreateAtStart())
                    .le(query.getCreateAtEnd() != null, OperationLog::getCreateAt, query.getCreateAtEnd());
        }
        wrapper.orderByDesc(OperationLog::getCreateAt);

        PageDTO<OperationLog> operationLogPage = operationLogMapper.selectPage(new PageDTO<>(page, pageSize), wrapper);
        return PageQueryUtils.map(operationLogPage, this::toVO);
    }

    private OperationLogVO toVO(OperationLog operationLog) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(operationLog.getId());
        vo.setTenantId(operationLog.getTenantId());
        vo.setTraceId(operationLog.getTraceId());
        vo.setUserName(operationLog.getUserName());
        vo.setOperation(operationLog.getOperation());
        vo.setClassName(operationLog.getClassName());
        vo.setMethodName(operationLog.getMethodName());
        vo.setRequestUri(operationLog.getRequestUri());
        vo.setRequestMethod(operationLog.getRequestMethod());
        vo.setRequestIp(operationLog.getRequestIp());
        vo.setUserAgent(operationLog.getUserAgent());
        vo.setRequestPayload(operationLog.getRequestPayload());
        vo.setResponsePayload(operationLog.getResponsePayload());
        vo.setSuccess(operationLog.getSuccess());
        vo.setDurationMs(operationLog.getDurationMs());
        vo.setErrorType(operationLog.getErrorType());
        vo.setErrorMessage(operationLog.getErrorMessage());
        vo.setCreateBy(operationLog.getCreateBy());
        vo.setCreateAt(operationLog.getCreateAt());
        return vo;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
