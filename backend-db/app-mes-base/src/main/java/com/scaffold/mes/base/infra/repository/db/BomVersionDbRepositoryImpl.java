package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.repository.BomVersionRepository;
import com.scaffold.mes.base.infra.mapper.db.BomVersionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * BomVersionDbRepositoryImpl BOM 版本仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_bom_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class BomVersionDbRepositoryImpl implements BomVersionRepository {

    private final BomVersionMapper mapper;

    private DbRepositorySupport<BomVersion> support() {
        return new DbRepositorySupport<>(mapper, BomVersion.class);
    }

    @Override
    public Optional<BomVersion> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public PageDTO<BomVersion> page(Long bomId, int page, int pageSize) {
        return support().queryPage(page, pageSize, support().wrapper()
                .eq(BomVersion::getBomId, bomId)
                .orderByDesc(BomVersion::getVersionNo));
    }

    @Override
    public List<BomVersion> listActive(Long bomId) {
        return mapper.selectList(support().wrapper()
                .eq(BomVersion::getBomId, bomId)
                .eq(BomVersion::getStatus, "ACTIVE"));
    }

    @Override
    public int nextVersionNo(Long bomId) {
        BomVersion latest = mapper.selectOne(support().wrapper()
                .eq(BomVersion::getBomId, bomId)
                .orderByDesc(BomVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    @Override
    public void save(BomVersion version) {
        support().save(version);
    }

    @Override
    public void update(BomVersion version) {
        support().update(version);
    }
}
