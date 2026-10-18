package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.PackContainer;
import com.scaffold.mes.exec.domain.repository.PackContainerRepository;
import com.scaffold.mes.exec.infra.mapper.db.PackContainerMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * PackContainerDbRepositoryImpl 包装容器仓储实现
 * <p>
 * 按容器号查重，装满后更新状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class PackContainerDbRepositoryImpl implements PackContainerRepository {

    private final PackContainerMapper mapper;

    private DbRepositorySupport<PackContainer> support() {
        return new DbRepositorySupport<>(mapper, PackContainer.class);
    }

    @Override
    public Optional<PackContainer> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<PackContainer> findByNo(String containerNo) {
        return support().findOne(new LambdaQueryWrapper<PackContainer>().eq(PackContainer::getContainerNo, containerNo));
    }

    @Override
    public List<PackContainer> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<PackContainer>().eq(PackContainer::getLotId, lotId)
                .orderByAsc(PackContainer::getContainerNo));
    }

    @Override
    public void save(PackContainer container) {
        support().save(container);
    }

    @Override
    public void update(PackContainer container) {
        support().update(container);
    }
}
