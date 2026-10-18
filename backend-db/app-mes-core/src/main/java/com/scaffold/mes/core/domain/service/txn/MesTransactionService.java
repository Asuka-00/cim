package com.scaffold.mes.core.domain.service.txn;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;
import com.scaffold.mes.core.domain.entity.txn.MesTxnReversal;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnRecordRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * MesTransactionService 统一事务层
 * <p>
 * 包住业务写入、成功流水与领域事件。失败流水在回滚后另起事务。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@Slf4j
public class MesTransactionService {

    private final MesTxnRecordRepository txnRepository;
    private final MesTxnReversalRepository reversalRepository;
    private final MesDomainEventRepository eventRepository;
    private final CurrentSessionProvider session;
    private final ObjectMapper objectMapper;
    private final MesRowLocker rowLocker;
    private final TransactionTemplate required;
    private final TransactionTemplate requiresNew;

    /**
     * 创建统一事务层。
     *
     * @param txnRepository 事务流水仓储
     * @param reversalRepository 撤销登记仓储
     * @param eventRepository 领域事件仓储
     * @param session 当前会话,补操作人
     * @param objectMapper 序列化幂等响应
     * @param rowLocker 聚合根 NOWAIT 行锁
     * @param transactionManager 成功路径用当前事务,失败流水用独立事务
     */
    public MesTransactionService(MesTxnRecordRepository txnRepository, MesTxnReversalRepository reversalRepository,
            MesDomainEventRepository eventRepository, CurrentSessionProvider session, ObjectMapper objectMapper,
            MesRowLocker rowLocker, PlatformTransactionManager transactionManager) {
        this.txnRepository = txnRepository;
        this.reversalRepository = reversalRepository;
        this.eventRepository = eventRepository;
        this.session = session;
        this.objectMapper = objectMapper;
        this.rowLocker = rowLocker;
        this.required = new TransactionTemplate(transactionManager);
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 执行一次业务事务。命中成功幂等键时返回原响应,不重复执行。
     *
     * @param command 事务描述
     * @param work 持锁后的业务写入
     * @param responseType 响应类型,无响应时用 Void
     * @return 业务结果或原成功响应
     * @throws BusinessException 对象被占用或锁等待超时
     */
    public <T> T execute(MesTxnCommand command, Supplier<T> work, Class<T> responseType) {
        String idemKey = idemKey(command);
        if (idemKey != null) {
            Hit<T> replay = replay(idemKey, responseType);
            if (replay != null) {
                return replay.value;
            }
        }
        try {
            return required.execute(status -> {
                if (idemKey != null) {
                    Hit<T> again = replay(idemKey, responseType);
                    if (again != null) {
                        return again.value;
                    }
                }
                for (MesLockTarget target : MesLockOrder.sort(command.getLocks())) {
                    rowLocker.lockNowait(target.kind(), target.id());
                }
                T result = work.get();
                saveSuccess(command, idemKey, result);
                return result;
            });
        } catch (DataIntegrityViolationException ex) {
            if (idemKey != null) {
                Hit<T> replay = replay(idemKey, responseType);
                if (replay != null) {
                    return replay.value;
                }
            }
            writeFailed(command, ex);
            throw ex;
        } catch (RuntimeException ex) {
            BusinessException translated = MesLockConflicts.translate(ex);
            writeFailed(command, translated == null ? ex : translated);
            if (translated != null) {
                throw translated;
            }
            throw ex;
        }
    }

    /**
     * 在当前事务中写入一条领域事件。
     *
     * @param eventType 事件类型
     * @param aggregateType 聚合类型
     * @param aggregateId 聚合主键
     * @param txnId 来源事务
     * @param payloadJson 事件载荷 JSON
     */
    public void publish(String eventType, String aggregateType, Long aggregateId, Long txnId, String payloadJson) {
        long id = AppIdGenerator.nextId();
        MesDomainEvent event = new MesDomainEvent();
        event.setId(id);
        event.setEventNo(Long.toString(id));
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setTxnId(txnId);
        event.setPayloadJson(payloadJson);
        event.setStatus("NEW");
        event.setRetryCount(0);
        eventRepository.save(event);
    }

    /**
     * 登记撤销并发布 TXN_REVERSED。须由业务事务调用。
     *
     * @param origTxnId 被撤销的事务主键
     * @param reasonCode 原因码,可空
     * @throws BusinessException 原事务不存在或已撤销
     */
    public void reverse(Long origTxnId, String reasonCode) {
        MesTxnRecord orig = txnRepository.findById(origTxnId)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.TXN_NOT_FOUND, "事务不存在"));
        long reversalTxnId = AppIdGenerator.nextId();
        MesTxnReversal reversal = new MesTxnReversal();
        reversal.setOrigTxnId(origTxnId);
        reversal.setReversalTxnId(reversalTxnId);
        reversal.setObjectType(orig.getObjectType());
        reversal.setObjectId(orig.getObjectId());
        reversal.setOperator(session.getCurrentUserNameOrDefault());
        reversal.setReasonCode(reasonCode);
        reversal.setReversedAt(LocalDateTime.now());
        try {
            reversalRepository.save(reversal);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(MesErrorCodes.TXN_ALREADY_REVERSED, "事务已撤销");
        }
        String payload = "{\"origTxnId\":" + origTxnId + ",\"reversalTxnId\":" + reversalTxnId
                + ",\"txnType\":\"" + orig.getTxnType() + "\",\"objectType\":\"" + orig.getObjectType()
                + "\",\"objectId\":" + orig.getObjectId() + "}";
        String aggregate = orig.getObjectType() == null ? "TXN" : orig.getObjectType();
        publish("TXN_REVERSED", aggregate, orig.getObjectId(), reversalTxnId, payload);
    }

    private <T> Hit<T> replay(String idemKey, Class<T> responseType) {
        return txnRepository.findSuccessByIdemKey(idemKey)
                .map(record -> new Hit<>(readResponse(record, responseType)))
                .orElse(null);
    }

    private record Hit<T>(T value) {
    }

    private <T> T readResponse(MesTxnRecord record, Class<T> responseType) {
        if (responseType == Void.class || responseType == void.class || record.getPayloadJson() == null) {
            return null;
        }
        try {
            JsonNode response = objectMapper.readTree(record.getPayloadJson()).get("response");
            return objectMapper.convertValue(response, responseType);
        } catch (Exception ex) {
            throw new BusinessException(MesErrorCodes.TXN_NOT_FOUND, "无法还原幂等响应", ex);
        }
    }

    private void saveSuccess(MesTxnCommand command, String idemKey, Object result) {
        MesTxnRecord record = baseRecord(command);
        record.setResult("SUCCESS");
        record.setIdemKey(idemKey);
        record.setPayloadJson(writePayload(result));
        txnRepository.save(record);
    }

    private void writeFailed(MesTxnCommand command, Exception ex) {
        try {
            requiresNew.executeWithoutResult(status -> {
                MesTxnRecord record = baseRecord(command);
                record.setResult("FAILED");
                record.setIdemKey(null);
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                record.setErrorMessage(message.length() > 500 ? message.substring(0, 500) : message);
                txnRepository.save(record);
            });
        } catch (RuntimeException logFailure) {
            log.warn("失败流水写入失败: {}", logFailure.getMessage());
        }
    }

    private MesTxnRecord baseRecord(MesTxnCommand command) {
        long id = AppIdGenerator.nextId();
        MesTxnRecord record = new MesTxnRecord();
        record.setId(id);
        record.setTxnNo(Long.toString(id));
        record.setTxnType(command.getTxnType());
        record.setObjectType(command.getObjectType());
        record.setObjectId(command.getObjectId());
        record.setObjectCode(command.getObjectCode());
        record.setRequestId(command.getRequestId());
        record.setSource(command.getSource() == null ? "ADMIN" : command.getSource());
        record.setReasonCode(command.getReasonCode());
        record.setOperator(command.getOperator() == null ? session.getCurrentUserNameOrDefault() : command.getOperator());
        record.setTxnTime(LocalDateTime.now());
        return record;
    }

    private String writePayload(Object result) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("response", result);
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String idemKey(MesTxnCommand command) {
        if (command.getRequestId() == null || command.getRequestId().isBlank() || command.getTxnType() == null) {
            return null;
        }
        return command.getTxnType() + ":" + command.getRequestId();
    }

}
