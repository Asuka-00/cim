package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotRelation;

/**
 * LotRelationRepository Lot 关系仓储
 * <p>
 * 只追加 hist_mes_lot_relation。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotRelationRepository {

    /**
     * 插入一条关系。
     *
     * @param relation 关系
     */
    void save(LotRelation relation);

    /**
     * 源 Lot 是否发生过封装投入。
     *
     * @param parentLotId 源 Lot
     * @return 有 PACKAGE_INPUT 时为 true
     */
    boolean existsPackageInput(Long parentLotId);

    /**
     * 列出从这个父 Lot 拆出的关系，按发生时间。
     *
     * @param parentLotId 父 Lot
     * @return SPLIT 关系，没有时为空列表
     */
    List<LotRelation> listSplits(Long parentLotId);

    /**
     * 找拆出这个子批的关系。有多条时取最早的一条。
     *
     * @param childLotId 子 Lot
     * @return SPLIT 关系，没有时为空
     */
    Optional<LotRelation> findSplitByChild(Long childLotId);

    /**
     * 列出合进这个目标 Lot 的关系，按发生时间。
     *
     * @param targetLotId 目标 Lot
     * @return MERGE 关系，没有时为空列表
     */
    List<LotRelation> listMergesInto(Long targetLotId);
}
