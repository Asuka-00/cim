package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.PackageInputRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * PackageInputApplicationService 封装投入
 * <p>
 * 源 Lot 扣减当前数量，封装 Lot 增加同样数量。源 Lot 扣到 0 时变为 CONSUMED。起始数量不改，工单仍按开批数量核算。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class PackageInputApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotRelationRepository relationRepository;
    private final LotHistoryApplicationService history;
    private final Clock clock;

    /**
     * 把源 Lot 的数量投入封装 Lot。
     *
     * @param targetLotId 封装 Lot
     * @param request 源 Lot 和数量
     * @throws BusinessException 数量不合法，或两边不是可投入状态
     */
    public void input(Long targetLotId, PackageInputRequest request) {
        if (targetLotId.equals(request.getSourceLotId())) {
            throw new BusinessException(MesExecErrorCodes.PACKAGE_INPUT_SOURCE, "不能把 Lot 投入自己");
        }
        if (request.getQty() == null || request.getQty() < 1) {
            throw new BusinessException(MesExecErrorCodes.PACKAGE_INPUT_QTY, "投入数量必须大于 0");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("PACKAGE_INPUT");
        command.setObjectType("LOT");
        command.setObjectId(targetLotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(targetLotId, request.getSourceLotId()).stream().sorted()
                .map(id -> new MesLockTarget("LOT", id)).toList());
        transactions.execute(command, () -> {
            apply(targetLotId, request.getSourceLotId(), request.getQty());
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long targetId, Long sourceId, long qty) {
        Lot target = require(targetId);
        Lot source = require(sourceId);
        if (target.getWoId() == null || !OPEN.contains(target.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.PACKAGE_INPUT_TARGET, "封装 Lot 必须已绑定工单并且仍在制");
        }
        if (!OPEN.contains(source.getStatus()) || (source.getHoldCount() != null && source.getHoldCount() > 0)) {
            throw new BusinessException(MesExecErrorCodes.PACKAGE_INPUT_SOURCE, "只有未 Hold 的在制 Lot 可以投入");
        }
        long sourceQty = source.getQty() == null ? 0L : source.getQty();
        if (qty > sourceQty) {
            throw new BusinessException(MesExecErrorCodes.PACKAGE_INPUT_QTY, "投入数量超过源 Lot 当前数量");
        }
        long targetBefore = target.getQty() == null ? 0L : target.getQty();
        String sourceBefore = source.getStatus();
        String targetBeforeStatus = target.getStatus();
        source.setQty(sourceQty - qty);
        if (source.getQty() == 0L) {
            source.setStatus("CONSUMED");
        }
        target.setQty(targetBefore + qty);
        lotRepository.update(source);
        lotRepository.update(target);
        LotRelation relation = new LotRelation();
        relation.setParentLotId(source.getId());
        relation.setChildLotId(target.getId());
        relation.setRelationType("PACKAGE_INPUT");
        relation.setQty(qty);
        relation.setUnitId(source.getQtyUnitId());
        relation.setOccurredAt(LocalDateTime.now(clock));
        relationRepository.save(relation);
        history.record(source, "PACKAGE_INPUT", null, null, null, sourceBefore, sourceQty);
        history.record(target, "PACKAGE_INPUT", null, null, null, targetBeforeStatus, targetBefore);
    }

    private Lot require(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
    }
}
