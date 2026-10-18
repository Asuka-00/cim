package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WaferMapImportRequest;
import com.scaffold.mes.exec.domain.entity.DieBinLog;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferMap;
import com.scaffold.mes.exec.domain.repository.DieBinLogRepository;
import com.scaffold.mes.exec.domain.repository.LotDieBinRepository;
import com.scaffold.mes.exec.domain.repository.WaferMapRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferMapApplicationService 导入 Wafer Map
 * <p>
 * CSV 每行是 binCode,dieQty,good。导入后该片只保留逐片 Bin，Lot 汇总行被清掉。重测生成新版本，并清掉上一版的当前标记。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferMapApplicationService {

    private final WaferRepository waferRepository;
    private final WaferMapRepository mapRepository;
    private final LotDieBinRepository binRepository;
    private final DieBinLogRepository logRepository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 列出某个 Lot 的全部 Map 版本。
     *
     * @param lotId Lot
     * @return 按片和版本排列
     */
    public List<WaferMap> list(Long lotId) {
        return mapRepository.listByLot(lotId);
    }

    /**
     * 导入一片的 Map。
     *
     * @param lotId Lot
     * @param request Wafer 和 CSV
     * @return 新版本号
     * @throws BusinessException Wafer 不属于该 Lot，或 CSV 没有可用行
     */
    @Transactional
    public int importMap(Long lotId, WaferMapImportRequest request) {
        Wafer wafer = waferRepository.findById(request.getWaferId())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
        if (!lotId.equals(wafer.getLotId())) {
            throw new BusinessException(MesExecErrorCodes.WAFER_MAP_LOT, "Wafer 不属于这个 Lot");
        }
        List<BinRow> rows = parse(request.getCsv());
        if (rows.isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_MAP_CSV, "Wafer Map 没有可用的 Bin 行");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String operator = session.getCurrentUserNameOrDefault();
        int version = 1;
        for (WaferMap previous : mapRepository.listByWafer(wafer.getId())) {
            version = Math.max(version, previous.getMapVersion() == null ? 1 : previous.getMapVersion() + 1);
            if (previous.getIsCurrent() != null && previous.getIsCurrent() == 1) {
                previous.setIsCurrent(0);
                mapRepository.update(previous);
            }
        }
        int total = 0;
        int good = 0;
        for (BinRow row : rows) {
            total += row.qty;
            if (row.good) {
                good += row.qty;
            }
        }
        WaferMap map = new WaferMap();
        map.setWaferId(wafer.getId());
        map.setLotId(lotId);
        map.setMapVersion(version);
        map.setIsCurrent(1);
        map.setMapFormat("CSV");
        map.setFileName(request.getFileName());
        map.setTotalDie(total);
        map.setGoodDie(good);
        map.setBadDie(total - good);
        map.setMapData(request.getCsv());
        map.setImportedBy(operator);
        map.setImportedAt(now);
        mapRepository.save(map);
        replaceBins(lotId, wafer.getId(), rows, operator, now);
        return version;
    }

    private void replaceBins(Long lotId, Long waferId, List<BinRow> rows, String operator, LocalDateTime now) {
        for (LotDieBin old : binRepository.listByLot(lotId)) {
            boolean summary = old.getWaferId() == null || old.getWaferId() == 0L;
            boolean sameWafer = waferId.equals(old.getWaferId());
            if (!summary && !sameWafer) {
                continue;
            }
            log(lotId, summary ? 0L : waferId, old.getBinCode(), -(old.getDieQty() == null ? 0 : old.getDieQty()), 0L,
                    summary ? "MIGRATE" : "WAFER_MAP", operator, now);
        }
        binRepository.deleteSummary(lotId);
        binRepository.deleteByWafer(waferId);
        for (BinRow row : rows) {
            LotDieBin bin = new LotDieBin();
            bin.setLotId(lotId);
            bin.setWaferId(waferId);
            bin.setBinCode(row.code);
            bin.setIsGood(row.good ? 1 : 0);
            bin.setDieQty((long) row.qty);
            bin.setSource("IMPORT");
            binRepository.save(bin);
            log(lotId, waferId, row.code, row.qty, row.qty, "WAFER_MAP", operator, now);
        }
    }

    private void log(Long lotId, Long waferId, String code, long delta, long qtyAfter, String reason, String operator,
            LocalDateTime now) {
        DieBinLog log = new DieBinLog();
        log.setLotId(lotId);
        log.setWaferId(waferId);
        log.setBinCode(code);
        log.setDelta(delta);
        log.setQtyAfter(qtyAfter);
        log.setReason(reason);
        log.setOperator(operator);
        log.setOccurredAt(now);
        logRepository.save(log);
    }

    private static List<BinRow> parse(String csv) {
        ArrayList<BinRow> rows = new ArrayList<>();
        if (csv == null) {
            return rows;
        }
        for (String raw : csv.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 3) {
                throw new BusinessException(MesExecErrorCodes.WAFER_MAP_CSV, "Wafer Map 每行需要编码、数量和良品标记");
            }
            String code = parts[0].trim();
            Long qty = parseLong(parts[1].trim());
            if (qty == null) {
                continue;
            }
            if (code.isEmpty() || qty < 0) {
                throw new BusinessException(MesExecErrorCodes.WAFER_MAP_CSV, "Wafer Map 的 Bin 行不合法");
            }
            rows.add(new BinRow(code, qty.intValue(), "1".equals(parts[2].trim())));
        }
        return rows;
    }

    private static Long parseLong(String text) {
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record BinRow(String code, int qty, boolean good) {
    }
}
