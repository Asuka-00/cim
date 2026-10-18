package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotShipRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotShipment;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotShipmentRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotShipApplicationService 出货
 * <p>
 * 只有已完成且未 Hold 的 Lot 可以整批出货。出货后状态为 SHIPPED。不锁工单，不改工单行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotShipApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotShipmentRepository shipmentRepository;
    private final LotHistoryApplicationService history;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 整批出货。
     *
     * @param lotId Lot
     * @param request 出货单号
     * @throws BusinessException 未完成、仍被 Hold，或出货单号重复
     */
    public void ship(Long lotId, LotShipRequest request) {
        if (shipmentRepository.findByNo(request.getShipNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.SHIP_NO_EXISTS, "出货单号已存在");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("SHIP");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        transactions.execute(command, () -> {
            apply(lotId, request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, LotShipRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"COMPLETED".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_SHIP_STATUS, "只有已完成的 Lot 可以出货");
        }
        if (lot.getHoldCount() != null && lot.getHoldCount() > 0) {
            throw new BusinessException(MesExecErrorCodes.LOT_SHIP_HOLD, "Hold 中的 Lot 不能出货");
        }
        lot.setStatus("SHIPPED");
        lotRepository.update(lot);
        LotShipment shipment = new LotShipment();
        shipment.setShipNo(request.getShipNo());
        shipment.setLotId(lotId);
        shipment.setWoId(lot.getWoId());
        shipment.setQty(lot.getQty());
        shipment.setUnitId(lot.getQtyUnitId());
        shipment.setShippedAt(LocalDateTime.now(clock));
        shipment.setShippedBy(session.getCurrentUserNameOrDefault());
        shipment.setRemark(request.getRemark());
        shipmentRepository.save(shipment);
        history.record(lot, "SHIP", null, request.getRemark(), null, "COMPLETED", lot.getQty());
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }
}
