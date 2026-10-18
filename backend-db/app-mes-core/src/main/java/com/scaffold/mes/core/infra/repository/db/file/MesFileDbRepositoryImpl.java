package com.scaffold.mes.core.infra.repository.db.file;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.file.MesFile;
import com.scaffold.mes.core.domain.repository.file.MesFileRepository;
import com.scaffold.mes.core.infra.mapper.db.file.MesFileMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesFileDbRepositoryImpl 文件元数据仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesFileDbRepositoryImpl implements MesFileRepository {

    private final MesFileMapper mapper;

    @Override
    public void save(MesFile file) {
        new DbRepositorySupport<>(mapper, MesFile.class).save(file);
    }

    @Override
    public List<MesFile> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<MesFile>()
                .eq(MesFile::getBizType, "LOT")
                .eq(MesFile::getBizId, lotId)
                .orderByAsc(MesFile::getId));
    }

    @Override
    public Optional<MesFile> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }
}
