package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.GenealogyLink;
import com.scaffold.mes.exec.domain.repository.GenealogyLinkRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * GenealogyLinkApplicationService 追溯关系
 * <p>
 * 同一上游、下游、关系类型和站点只插入一次，数量保持首次值。原事务已撤销且关系再次发生时，只把 txn_id 改指向新事务。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class GenealogyLinkApplicationService {

    private static final Set<String> OBJECTS = Set.of(
            "MATERIAL_LOT", "LOT", "WAFER", "STRIP", "CONTAINER", "EQUIPMENT", "TOOL");
    private static final Set<String> LINKS = Set.of(
            "CREATE", "PROCESS", "CONSUME", "SPLIT", "MERGE", "CONVERT", "PACK", "OPEN_LOT");

    private final GenealogyLinkRepository repository;
    private final MesTxnReversalRepository reversalRepository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 记录一条关系。已存在且原事务未撤销时什么都不改。
     *
     * @param parentType 上游类型
     * @param parentId 上游对象
     * @param parentCode 上游编码，可空
     * @param childType 下游类型
     * @param childId 下游对象
     * @param childCode 下游编码，可空
     * @param linkType 关系类型
     * @param stepId 站点，空则按 0
     * @param qty 首次数量，可空
     * @param txnId 本次事务，可空
     * @throws BusinessException 对象类型或关系类型不合法
     */
    @Transactional
    public void link(String parentType, Long parentId, String parentCode, String childType, Long childId, String childCode,
            String linkType, Long stepId, BigDecimal qty, Long txnId) {
        if (!OBJECTS.contains(parentType)) {
            throw new BusinessException(MesExecErrorCodes.GENEALOGY_PARENT_TYPE_INVALID, "追溯上游类型不合法");
        }
        if (!OBJECTS.contains(childType)) {
            throw new BusinessException(MesExecErrorCodes.GENEALOGY_CHILD_TYPE_INVALID, "追溯下游类型不合法");
        }
        if (!LINKS.contains(linkType)) {
            throw new BusinessException(MesExecErrorCodes.GENEALOGY_LINK_TYPE_INVALID, "追溯关系类型不合法");
        }
        long step = stepId == null ? 0L : stepId;
        GenealogyLink existing = repository.find(parentType, parentId, childType, childId, linkType, step).orElse(null);
        if (existing != null) {
            if (txnId != null && reversalRepository.existsByOrigTxnId(existing.getTxnId())) {
                repository.backfillTxnId(existing.getId(), txnId);
            }
            return;
        }
        GenealogyLink link = new GenealogyLink();
        link.setParentType(parentType);
        link.setParentId(parentId);
        link.setParentCode(parentCode);
        link.setChildType(childType);
        link.setChildId(childId);
        link.setChildCode(childCode);
        link.setLinkType(linkType);
        link.setStepId(step);
        link.setQty(qty);
        link.setTxnId(txnId);
        link.setOperator(session.getCurrentUserNameOrDefault());
        link.setOccurredAt(LocalDateTime.now(clock));
        repository.save(link);
    }
}
