package com.scaffold.mes.exec.domain.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.share.exception.BusinessException;

/**
 * WipImportCsv 把期初在制 CSV 拆成导入行
 * <p>
 * 第一行是表头，列名与 {@link WipImportRequest} 字段相同，另加 woId。空行跳过。单元格不能含逗号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
final class WipImportCsv {

    private static final Set<String> COLUMNS = Set.of(
            "woId", "lotNo", "qty", "seqNo", "stepId",
            "passCount", "priorStepId", "priorPassCount", "holdReasonCode",
            "stepEnterAt", "lastTrackOutAt", "reworkId", "reworkSeqNo",
            "locationId", "recipeVersionId", "timeLinkId", "timeLinkStartAt",
            "waferCode", "slotNo", "waferStatus", "scrapReason");

    private WipImportCsv() {
    }

    /**
     * 解析 CSV 文本。
     *
     * @param csv 文件内容
     * @return 数据行，行号从 1 开始
     * @throws BusinessException 没有表头、表头无法识别、没有数据行，或某一行无法解析
     */
    static List<ImportWipRowRequest> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "至少要有一行");
        }
        String[] lines = csv.split("\\R", -1);
        String[] header = null;
        List<ImportWipRowRequest> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (header == null) {
                header = splitHeader(stripBom(line));
                continue;
            }
            rows.add(row(header, line, rows.size() + 1));
        }
        if (header == null || rows.isEmpty()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "至少要有一行");
        }
        return rows;
    }

    private static String[] splitHeader(String line) {
        String[] header = line.split(",", -1);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < header.length; i++) {
            header[i] = header[i].trim();
            if (!COLUMNS.contains(header[i]) || !seen.add(header[i])) {
                throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入文件表头无法识别");
            }
        }
        if (!seen.containsAll(Set.of("woId", "lotNo", "qty", "seqNo", "stepId"))) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入文件表头无法识别");
        }
        return header;
    }

    private static ImportWipRowRequest row(String[] header, String line, int rowNo) {
        String[] cells = line.split(",", -1);
        if (cells.length != header.length) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "第" + rowNo + "行无法解析");
        }
        ImportWipRowRequest request = new ImportWipRowRequest();
        WipImportRequest writing = new WipImportRequest();
        request.setRow(writing);
        request.setRowNo(rowNo);
        try {
            for (int i = 0; i < header.length; i++) {
                put(request, writing, header[i], cells[i].trim());
            }
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "第" + rowNo + "行无法解析");
        }
        if (request.getWoId() == null || writing.getLotNo() == null || writing.getLotNo().isBlank()
                || writing.getQty() == null || writing.getSeqNo() == null || writing.getStepId() == null) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "第" + rowNo + "行无法解析");
        }
        return request;
    }

    private static void put(ImportWipRowRequest request, WipImportRequest writing, String name, String value) {
        if (value.isEmpty()) {
            return;
        }
        switch (name) {
            case "woId" -> request.setWoId(Long.valueOf(value));
            case "lotNo" -> writing.setLotNo(value);
            case "qty" -> writing.setQty(Long.valueOf(value));
            case "seqNo" -> writing.setSeqNo(Integer.valueOf(value));
            case "stepId" -> writing.setStepId(Long.valueOf(value));
            case "passCount" -> writing.setPassCount(Integer.valueOf(value));
            case "priorStepId" -> writing.setPriorStepId(Long.valueOf(value));
            case "priorPassCount" -> writing.setPriorPassCount(Integer.valueOf(value));
            case "holdReasonCode" -> writing.setHoldReasonCode(value);
            case "stepEnterAt" -> writing.setStepEnterAt(LocalDateTime.parse(value));
            case "lastTrackOutAt" -> writing.setLastTrackOutAt(LocalDateTime.parse(value));
            case "reworkId" -> writing.setReworkId(Long.valueOf(value));
            case "reworkSeqNo" -> writing.setReworkSeqNo(Integer.valueOf(value));
            case "locationId" -> writing.setLocationId(Long.valueOf(value));
            case "recipeVersionId" -> writing.setRecipeVersionId(Long.valueOf(value));
            case "timeLinkId" -> writing.setTimeLinkId(Long.valueOf(value));
            case "timeLinkStartAt" -> writing.setTimeLinkStartAt(LocalDateTime.parse(value));
            case "waferCode" -> writing.setWaferCode(value);
            case "slotNo" -> writing.setSlotNo(Integer.valueOf(value));
            case "waferStatus" -> writing.setWaferStatus(value);
            case "scrapReason" -> writing.setScrapReason(value);
            default -> throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入文件表头无法识别");
        }
    }

    private static String stripBom(String line) {
        if (line.startsWith("\uFEFF")) {
            return line.substring(1);
        }
        return line;
    }
}
