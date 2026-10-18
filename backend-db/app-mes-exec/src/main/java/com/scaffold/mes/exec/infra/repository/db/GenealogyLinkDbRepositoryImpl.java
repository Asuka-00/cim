package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.GenealogyLink;
import com.scaffold.mes.exec.domain.repository.GenealogyLinkRepository;
import com.scaffold.mes.exec.infra.mapper.db.GenealogyLinkMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * GenealogyLinkDbRepositoryImpl 追溯关系仓储实现
 * <p>
 * 插入走只增基座。txn_id 回填走 Mapper 上的专用 SQL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class GenealogyLinkDbRepositoryImpl implements GenealogyLinkRepository {

    private final GenealogyLinkMapper mapper;

    private AppendOnlyRepositorySupport<GenealogyLink> support() {
        return new AppendOnlyRepositorySupport<>(mapper, GenealogyLink.class);
    }

    @Override
    public Optional<GenealogyLink> find(String parentType, Long parentId, String childType, Long childId, String linkType, Long stepId) {
        return support().findOne(new LambdaQueryWrapper<GenealogyLink>()
                .eq(GenealogyLink::getParentType, parentType)
                .eq(GenealogyLink::getParentId, parentId)
                .eq(GenealogyLink::getChildType, childType)
                .eq(GenealogyLink::getChildId, childId)
                .eq(GenealogyLink::getLinkType, linkType)
                .eq(GenealogyLink::getStepId, stepId));
    }

    @Override
    public void save(GenealogyLink link) {
        support().save(link);
    }

    @Override
    public int backfillTxnId(Long id, Long txnId) {
        return mapper.backfillTxnId(id, txnId);
    }
}
