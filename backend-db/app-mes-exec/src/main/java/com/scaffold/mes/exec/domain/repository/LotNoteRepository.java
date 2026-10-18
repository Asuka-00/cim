package com.scaffold.mes.exec.domain.repository;

import java.util.List;

import com.scaffold.mes.exec.domain.entity.LotNote;

/**
 * LotNoteRepository Lot 备注仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotNoteRepository {

    /**
     * 插入。
     *
     * @param note 备注
     */
    void save(LotNote note);

    /**
     * 列出某 Lot 的备注，置顶在前。
     *
     * @param lotId Lot
     * @return 备注
     */
    List<LotNote> listByLot(Long lotId);
}
