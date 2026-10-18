package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotNote;
import com.scaffold.mes.exec.domain.repository.LotNoteRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotNoteMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotNoteDbRepositoryImpl Lot 备注仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotNoteDbRepositoryImpl implements LotNoteRepository {

    private final LotNoteMapper mapper;

    @Override
    public void save(LotNote note) {
        new DbRepositorySupport<>(mapper, LotNote.class).save(note);
    }

    @Override
    public List<LotNote> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotNote>()
                .eq(LotNote::getLotId, lotId)
                .orderByDesc(LotNote::getPinned)
                .orderByDesc(LotNote::getId));
    }
}
