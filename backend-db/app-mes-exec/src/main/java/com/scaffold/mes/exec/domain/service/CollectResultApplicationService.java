package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.CollectResult;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.CollectResultRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.CollectSpecPort;
import com.scaffold.share.integration.mes.base.response.CollectLimit;

import lombok.RequiredArgsConstructor;

/**
 * CollectResultApplicationService 采集录入
 * <p>
 * 录入走统一事务，类型为 COLLECT。只接受加工中的 Lot。有规格时按上下限判定。超规格且打开自动 Hold 时挂起。没有规格时记 NA。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class CollectResultApplicationService {

    private final LotRepository lots;
    private final CollectResultRepository results;
    private final CollectSpecPort specs;
    private final LotHoldApplicationService holds;
    private final MesTransactionService transactions;
    private final Clock clock;

    /**
     * 录入一条原始数值。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @param valueNum 原始数值
     * @throws BusinessException Lot 不存在、不在加工中，或没有数值
     */
    public void record(Long lotId, Long itemId, BigDecimal valueNum) {
        if (valueNum == null) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_VALUE, "采集值没有填写");
        }
        Lot loaded = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("COLLECT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setObjectCode(loaded.getLotNo());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            applyRecord(lotId, itemId, valueNum);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void applyRecord(Long lotId, Long itemId, BigDecimal valueNum) {
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"RUNNING".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_NOT_RUNNING, "只有加工中的 Lot 可以录入");
        }
        CollectResult result = new CollectResult();
        result.setLotId(lotId);
        result.setItemId(itemId);
        result.setValueNum(valueNum);
        result.setRecordType("ORIGINAL");
        CollectLimit limit = specs.limit(lot.getRouteVersionId(), lot.getRouteSeqNo(), lot.getStepId(), itemId,
                lot.getProductId()).orElse(null);
        result.setJudgeResult(judge(valueNum, limit));
        if (limit != null) {
            result.setLsl(limit.lsl());
            result.setUsl(limit.usl());
        }
        result.setProductId(lot.getProductId());
        result.setRouteSeqNo(lot.getRouteSeqNo());
        result.setStepId(lot.getStepId());
        result.setPassNo(lot.getPassNo());
        result.setCollectedAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        results.save(result);
        if (limit != null && limit.autoHold() && result.getJudgeResult().startsWith("OOS")) {
            holds.holdSystem(lotId, "OOS", limit.reasonCode(), null);
        }
    }

    private static String judge(BigDecimal value, CollectLimit limit) {
        if (limit == null) {
            return "NA";
        }
        if (limit.lsl() != null && value.compareTo(limit.lsl()) < 0) {
            return "OOS_LOW";
        }
        if (limit.usl() != null && value.compareTo(limit.usl()) > 0) {
            return "OOS_HIGH";
        }
        return "OK";
    }

    /**
     * 更正当前经过的有效值。原记录保留，追加一条更正。打开自动 Hold 时，超规格挂起，回到规格内则释放。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @param valueNum 更正后的数值
     * @throws BusinessException Lot 不在加工中，没有可更正的值，或没有填写数值
     */
    @Transactional
    public void correct(Long lotId, Long itemId, BigDecimal valueNum) {
        if (valueNum == null) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_VALUE, "采集值没有填写");
        }
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"RUNNING".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_NOT_RUNNING, "只有加工中的 Lot 可以更正采集值");
        }
        CollectResult latest = results.latest(lotId, itemId, lot.getRouteSeqNo(), lot.getPassNo()).orElse(null);
        if (latest == null || "VOID".equals(latest.getRecordType())) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_CORRECT, "没有可更正的采集值");
        }
        CollectLimit limit = specs.limit(lot.getRouteVersionId(), lot.getRouteSeqNo(), lot.getStepId(), itemId,
                lot.getProductId()).orElse(null);
        CollectResult row = new CollectResult();
        row.setLotId(lotId);
        row.setItemId(itemId);
        row.setValueNum(valueNum);
        row.setRecordType("CORRECTION");
        row.setRefId(latest.getId());
        row.setJudgeResult(judge(valueNum, limit));
        if (limit != null) {
            row.setLsl(limit.lsl());
            row.setUsl(limit.usl());
        }
        row.setProductId(lot.getProductId());
        row.setRouteSeqNo(lot.getRouteSeqNo());
        row.setStepId(lot.getStepId());
        row.setPassNo(lot.getPassNo());
        row.setCollectedAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        results.save(row);
        if (limit != null && limit.autoHold()) {
            if (row.getJudgeResult().startsWith("OOS")) {
                holds.holdSystem(lotId, "OOS", limit.reasonCode(), null);
            } else {
                holds.releaseSystem(lotId, "OOS", limit.reasonCode());
            }
        }
    }

    /**
     * 撤销这次采集。作废最后一条有效值，并释放它挂上的超规格 Hold。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @throws BusinessException Lot 不在加工中，或没有可作废的值
     */
    public void voidLatest(Long lotId, Long itemId) {
        Lot loaded = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"RUNNING".equals(loaded.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_NOT_RUNNING, "只有加工中的 Lot 可以作废采集值");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("UNDO_COLLECT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setObjectCode(loaded.getLotNo());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            applyVoid(lotId, itemId);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void applyVoid(Long lotId, Long itemId) {
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        CollectResult latest = results.latest(lotId, itemId, lot.getRouteSeqNo(), lot.getPassNo()).orElse(null);
        if (latest == null || "VOID".equals(latest.getRecordType())) {
            throw new BusinessException(MesExecErrorCodes.COLLECT_VOID, "没有可作废的采集值");
        }
        CollectResult row = new CollectResult();
        row.setLotId(lotId);
        row.setItemId(itemId);
        row.setRecordType("VOID");
        row.setRefId(latest.getId());
        row.setJudgeResult("NA");
        row.setProductId(lot.getProductId());
        row.setRouteSeqNo(lot.getRouteSeqNo());
        row.setStepId(lot.getStepId());
        row.setPassNo(lot.getPassNo());
        row.setCollectedAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        results.save(row);
        if (latest.getJudgeResult() != null && latest.getJudgeResult().startsWith("OOS")) {
            CollectLimit limit = specs.limit(lot.getRouteVersionId(), lot.getRouteSeqNo(), lot.getStepId(), itemId,
                lot.getProductId()).orElse(null);
            if (limit != null && limit.autoHold()) {
                holds.releaseSystem(lotId, "OOS", limit.reasonCode());
            }
        }
    }

    /**
     * 这个采集点当前的有效值。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @param routeSeqNo 步骤序号
     * @param passNo 经过次数
     * @return 最后一条的数值。没有记录，或最后一条已作废时为空
     */
    public Optional<BigDecimal> effective(Long lotId, Long itemId, Integer routeSeqNo, Integer passNo) {
        CollectResult latest = results.latest(lotId, itemId, routeSeqNo, passNo).orElse(null);
        if (latest == null || "VOID".equals(latest.getRecordType())) {
            return Optional.empty();
        }
        return Optional.ofNullable(latest.getValueNum());
    }
}
