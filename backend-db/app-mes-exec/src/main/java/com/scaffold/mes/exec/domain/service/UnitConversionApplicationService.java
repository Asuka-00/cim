package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.UnitConversion;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.UnitConversionRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.UnitConvertProvider;
import com.scaffold.share.integration.mes.base.response.UnitConvertFacts;

import lombok.RequiredArgsConstructor;

/**
 * UnitConversionApplicationService 单位转换
 * <p>
 * 只在转换站点的出站事务里调用。按系数算参考数量，偏差超限必须有原因。然后切换 Lot 的数量和单位，存活片标成 CONVERTED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UnitConversionApplicationService {

    private static final Set<String> ALIVE = Set.of("NORMAL", "RUNNING");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final UnitConvertProvider conversions;
    private final UnitConversionRepository repository;
    private final WaferRepository waferRepository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 当前步骤是转换站点时改数量和单位，并写历史。
     *
     * @param lot 正在出站的 Lot，数量和单位会被改掉
     * @param request 实际数量和确认原因，可空
     * @throws BusinessException 没填数量、没有换算系数，或偏差未确认
     */
    public void apply(Lot lot, TrackOutRequest request) {
        UnitConvertFacts facts = conversions.facts(lot.getRouteVersionId(), lot.getRouteSeqNo(), lot.getProductId(),
                lot.getQtyUnitId());
        if (!facts.convertStep()) {
            return;
        }
        Long qtyAfter = request == null ? null : request.getQtyAfter();
        if (qtyAfter == null || qtyAfter < 0) {
            throw new BusinessException(MesExecErrorCodes.UNIT_CONVERT_QTY_REQUIRED, "转换站点必须填写转换后数量");
        }
        if (facts.factor() == null || facts.toUnitId() == null) {
            throw new BusinessException(MesExecErrorCodes.UNIT_CONVERT_FACTOR_MISSING, "产品没有这条单位换算");
        }
        long qtyBefore = lot.getQty() == null ? 0L : lot.getQty();
        long reference = BigDecimal.valueOf(qtyBefore).multiply(facts.factor()).setScale(0, RoundingMode.HALF_UP).longValue();
        BigDecimal deviation = deviation(reference, qtyAfter);
        BigDecimal tolerance = facts.tolerancePct() == null ? BigDecimal.ZERO : facts.tolerancePct();
        String reason = request.getConfirmReason();
        if (deviation.compareTo(tolerance) > 0 && (reason == null || reason.isBlank())) {
            throw new BusinessException(MesExecErrorCodes.UNIT_CONVERT_DEVIATION, "转换数量偏差超过允许范围");
        }
        UnitConversion row = new UnitConversion();
        row.setLotId(lot.getId());
        row.setRouteSeqNo(lot.getRouteSeqNo());
        row.setStepId(lot.getStepId());
        row.setFromUnitId(lot.getQtyUnitId());
        row.setToUnitId(facts.toUnitId());
        row.setQtyBefore(qtyBefore);
        row.setQtyReference(reference);
        row.setQtyAfter(qtyAfter);
        row.setLossQty(reference - qtyAfter);
        row.setDeviationPct(deviation);
        row.setFactorUsed(facts.factor());
        row.setScrapArchivedQty(lot.getCurrentScrapQty());
        row.setConfirmReason(reason);
        row.setOperator(session.getCurrentUserNameOrDefault());
        row.setOccurredAt(LocalDateTime.now(clock));
        repository.save(row);
        lot.setQty(qtyAfter);
        lot.setQtyUnitId(facts.toUnitId());
        lot.setCurrentScrapQty(0L);
        for (Wafer wafer : waferRepository.listByLot(lot.getId())) {
            if (ALIVE.contains(wafer.getStatus())) {
                wafer.setStatus("CONVERTED");
                waferRepository.update(wafer);
            }
        }
    }

    private static BigDecimal deviation(long reference, long qtyAfter) {
        if (reference == 0) {
            return qtyAfter == 0 ? BigDecimal.ZERO : HUNDRED;
        }
        return BigDecimal.valueOf(Math.abs(qtyAfter - reference)).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(reference), 4, RoundingMode.HALF_UP);
    }
}
