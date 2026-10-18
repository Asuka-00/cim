package com.scaffold.mes.exec.domain.repository;

import java.util.List;

import com.scaffold.mes.exec.domain.entity.WaferMap;

/**
 * WaferMapRepository Wafer Map 仓储
 * <p>
 * 读写 mes_wafer_map。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WaferMapRepository {

    /**
     * 列出某一片的全部版本。
     *
     * @param waferId Wafer
     * @return 版本列表
     */
    List<WaferMap> listByWafer(Long waferId);

    /**
     * 列出某个 Lot 的全部版本。
     *
     * @param lotId Lot
     * @return 按片和版本排列
     */
    List<WaferMap> listByLot(Long lotId);

    /**
     * 插入一版。
     *
     * @param map Map
     */
    void save(WaferMap map);

    /**
     * 更新一版。重测时用来清掉旧的当前标记。
     *
     * @param map 已加载的 Map
     */
    void update(WaferMap map);
}
