package com.scaffold.mes.exec.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotChildResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotGenealogyApplicationService 正向追溯
 * <p>
 * 按父 Lot 列出显式拆出的子批，或按子批找回父 Lot。封装投入和合批不在这里。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotGenealogyApplicationService {

    private final LotRepository lots;
    private final LotRelationRepository relations;

    /**
     * 列出子批。
     *
     * @param parentLotId 父 Lot
     * @return 子批，没有时为空列表
     * @throws BusinessException 父 Lot 不存在
     */
    public List<LotChildResponse> children(Long parentLotId) {
        if (lots.findById(parentLotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        List<LotChildResponse> children = new ArrayList<>();
        for (LotRelation relation : relations.listSplits(parentLotId)) {
            Lot child = lots.findById(relation.getChildLotId()).orElse(null);
            children.add(new LotChildResponse(relation.getChildLotId(), child == null ? null : child.getLotNo(),
                    relation.getQty()));
        }
        return children;
    }

    /**
     * 找回拆出这个子批的父 Lot。
     *
     * @param childLotId 子 Lot
     * @return 父 Lot。这个批不是拆出来的时为空
     * @throws BusinessException 子 Lot 不存在
     */
    public Optional<LotChildResponse> parent(Long childLotId) {
        if (lots.findById(childLotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        LotRelation relation = relations.findSplitByChild(childLotId).orElse(null);
        if (relation == null) {
            return Optional.empty();
        }
        Lot parent = lots.findById(relation.getParentLotId()).orElse(null);
        return Optional.of(new LotChildResponse(relation.getParentLotId(), parent == null ? null : parent.getLotNo(),
                relation.getQty()));
    }

    /**
     * 列出来合入这个目标 Lot 的来源批。
     *
     * @param targetLotId 目标 Lot
     * @return 来源批，没有时为空列表
     * @throws BusinessException 目标 Lot 不存在
     */
    public List<LotChildResponse> mergeSources(Long targetLotId) {
        if (lots.findById(targetLotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        List<LotChildResponse> sources = new ArrayList<>();
        for (LotRelation relation : relations.listMergesInto(targetLotId)) {
            Lot source = lots.findById(relation.getParentLotId()).orElse(null);
            sources.add(new LotChildResponse(relation.getParentLotId(), source == null ? null : source.getLotNo(),
                    relation.getQty()));
        }
        return sources;
    }
}
