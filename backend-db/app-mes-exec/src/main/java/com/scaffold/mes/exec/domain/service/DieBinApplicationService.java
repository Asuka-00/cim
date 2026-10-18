package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.DieBinLine;
import com.scaffold.mes.exec.domain.entity.DieBinLog;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.repository.DieBinLogRepository;
import com.scaffold.mes.exec.domain.repository.LotDieBinRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * DieBinApplicationService Die Bin 汇总
 * <p>
 * 没有逐片明细时，用汇总行替换该 Lot 的 Bin。良品合计必须等于 Lot 数量。每次替换写流水。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class DieBinApplicationService {

    private final LotRepository lotRepository;
    private final LotDieBinRepository binRepository;
    private final DieBinLogRepository logRepository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 列出某个 Lot 的 Bin 行。
     *
     * @param lotId Lot
     * @return 汇总行和逐片行
     */
    public List<LotDieBin> list(Long lotId) {
        return binRepository.listByLot(lotId);
    }

    /**
     * 替换 Lot 的 Bin 汇总。
     *
     * @param lotId Lot
     * @param lines Bin 行
     * @throws BusinessException 已有逐片明细、行不合法，或良品合计与数量不一致
     */
    @Transactional
    public void replaceSummary(Long lotId, List<DieBinLine> lines) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.DIE_BIN_LINE_INVALID, "至少要有一条 Bin");
        }
        HashSet<String> codes = new HashSet<>();
        long good = 0L;
        for (DieBinLine line : lines) {
            if (line.getBinCode() == null || line.getBinCode().isBlank() || line.getDieQty() == null || line.getDieQty() < 0
                    || !codes.add(line.getBinCode())) {
                throw new BusinessException(MesExecErrorCodes.DIE_BIN_LINE_INVALID, "Bin 编码重复、为空，或数量为负");
            }
            if (Boolean.TRUE.equals(line.getGood())) {
                good += line.getDieQty();
            }
        }
        List<LotDieBin> existing = binRepository.listByLot(lotId);
        if (existing.stream().anyMatch(row -> row.getWaferId() != null && row.getWaferId() != 0L)) {
            throw new BusinessException(MesExecErrorCodes.DIE_BIN_DETAIL_EXISTS, "已有逐片明细，不能写汇总");
        }
        long qty = lot.getQty() == null ? 0L : lot.getQty();
        if (good != qty) {
            throw new BusinessException(MesExecErrorCodes.DIE_BIN_QTY_MISMATCH, "良品 Bin 合计必须等于 Lot 数量");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String operator = session.getCurrentUserNameOrDefault();
        for (LotDieBin old : existing) {
            log(lotId, old.getBinCode(), -(old.getDieQty() == null ? 0L : old.getDieQty()), 0L, operator, now);
        }
        binRepository.deleteSummary(lotId);
        for (DieBinLine line : lines) {
            LotDieBin row = new LotDieBin();
            row.setLotId(lotId);
            row.setWaferId(0L);
            row.setBinCode(line.getBinCode());
            row.setBinName(line.getBinName());
            row.setIsGood(Boolean.TRUE.equals(line.getGood()) ? 1 : 0);
            row.setDieQty(line.getDieQty());
            row.setSource("MANUAL");
            binRepository.save(row);
            log(lotId, line.getBinCode(), line.getDieQty(), line.getDieQty(), operator, now);
        }
    }

    private void log(Long lotId, String binCode, long delta, long qtyAfter, String operator, LocalDateTime now) {
        DieBinLog log = new DieBinLog();
        log.setLotId(lotId);
        log.setWaferId(0L);
        log.setBinCode(binCode);
        log.setDelta(delta);
        log.setQtyAfter(qtyAfter);
        log.setReason("MANUAL");
        log.setOperator(operator);
        log.setOccurredAt(now);
        logRepository.save(log);
    }
}
