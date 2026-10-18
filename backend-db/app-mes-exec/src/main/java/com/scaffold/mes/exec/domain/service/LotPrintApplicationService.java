package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.print.PrintLog;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.repository.print.PrintLogRepository;
import com.scaffold.mes.core.domain.repository.print.PrintTemplateRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotPrintRequest;
import com.scaffold.mes.exec.domain.dto.LotPrintResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotPrintApplicationService 记录 Lot 打印
 * <p>
 * 模板必须已存在且启用。同一 Lot 和同一模板再次打印记成重打。可以列出这个 Lot 的打印记录。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotPrintApplicationService {

    private final LotRepository lots;

    private final PrintTemplateRepository templates;

    private final PrintLogRepository logs;

    private final Clock clock;

    /**
     * 记一条打印。
     *
     * @param lotId Lot
     * @param request 模板编码和份数
     * @throws BusinessException Lot 不存在，模板不存在或已停用，或份数小于 1
     */
    @Transactional
    public void record(Long lotId, LotPrintRequest request) {
        int copies = request.getCopies() == null ? 1 : request.getCopies();
        if (copies < 1) {
            throw new BusinessException(MesExecErrorCodes.PRINT_COPIES, "打印份数必须大于 0");
        }
        Lot lot = lots.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        PrintTemplate template = templates.findByCode(request.getTplCode()).orElse(null);
        if (template == null || !Integer.valueOf(1).equals(template.getEnabled())) {
            throw new BusinessException(MesErrorCodes.PRINT_TEMPLATE_INVALID, "打印模板不存在或已停用");
        }
        PrintLog log = new PrintLog();
        log.setTplId(template.getId());
        log.setTplType(template.getTplType());
        log.setBizType("LOT");
        log.setBizId(lot.getId());
        log.setBizCode(lot.getLotNo());
        log.setCopies(copies);
        log.setIsReprint(logs.exists("LOT", lot.getId(), template.getId()) ? 1 : 0);
        log.setPrintedAt(LocalDateTime.now(clock));
        logs.save(log);
    }

    /**
     * 列出这个 Lot 的打印记录。
     *
     * @param lotId Lot
     * @return 按打印时间从早到晚。没有时为空
     * @throws BusinessException Lot 不存在
     */
    public List<LotPrintResponse> list(Long lotId) {
        if (lots.findById(lotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        return logs.listByBiz("LOT", lotId).stream()
                .map(row -> new LotPrintResponse(row.getTplId(), row.getCopies(), row.getIsReprint(), row.getPrintedAt()))
                .toList();
    }
}
