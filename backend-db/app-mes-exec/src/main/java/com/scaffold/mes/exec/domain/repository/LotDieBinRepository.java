package com.scaffold.mes.exec.domain.repository;

import java.util.List;

import com.scaffold.mes.exec.domain.entity.LotDieBin;

/**
 * LotDieBinRepository Die Bin 仓储
 * <p>
 * 读写 mes_lot_die_bin。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotDieBinRepository {

    /**
     * 列出某个 Lot 的全部 Bin 行。
     *
     * @param lotId Lot
     * @return 汇总行和逐片行
     */
    List<LotDieBin> listByLot(Long lotId);

    /**
     * 删掉某个 Lot 的汇总行。
     *
     * @param lotId Lot
     */
    void deleteSummary(Long lotId);

    /**
     * 删掉某一片上的 Bin 行。
     *
     * @param waferId Wafer
     */
    void deleteByWafer(Long waferId);

    /**
     * 插入一行。
     *
     * @param bin Bin 行
     */
    void save(LotDieBin bin);
}
