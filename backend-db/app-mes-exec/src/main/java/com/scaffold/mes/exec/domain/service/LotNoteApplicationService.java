package com.scaffold.mes.exec.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotNoteRequest;
import com.scaffold.mes.exec.domain.entity.LotNote;
import com.scaffold.mes.exec.domain.repository.LotNoteRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotNoteApplicationService Lot 备注
 * <p>
 * 任何已存在的 Lot 都可以追加备注，包括 Hold 中的批。置顶备注排在前面。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotNoteApplicationService {

    private static final Set<String> TYPES = Set.of("NOTE", "WARNING", "ENGINEERING", "TAG");

    private final LotNoteRepository repository;
    private final LotRepository lotRepository;

    /**
     * 追加备注。
     *
     * @param lotId Lot
     * @param request 类型和内容
     * @throws BusinessException Lot 不存在、内容为空或类型不合法
     */
    @Transactional
    public void add(Long lotId, LotNoteRequest request) {
        if (lotRepository.findById(lotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOTE_CONTENT_REQUIRED, "备注内容不能为空");
        }
        String type = request.getNoteType() == null || request.getNoteType().isBlank() ? "NOTE" : request.getNoteType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOTE_TYPE_INVALID, "备注类型不合法");
        }
        LotNote note = new LotNote();
        note.setLotId(lotId);
        note.setNoteType(type);
        note.setContent(request.getContent());
        note.setPinned(request.getPinned() != null && request.getPinned() == 1 ? 1 : 0);
        repository.save(note);
    }

    /**
     * 列出备注，置顶在前。
     *
     * @param lotId Lot
     * @return 备注
     * @throws BusinessException Lot 不存在
     */
    public List<LotNote> list(Long lotId) {
        if (lotRepository.findById(lotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        return repository.listByLot(lotId);
    }
}
