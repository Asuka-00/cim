package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.Strip;

/**
 * StripRepository Strip 仓储
 * <p>
 * 读写 mes_strip。编号在租户内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StripRepository {

    /**
     * 按编号查询。
     *
     * @param stripCode 编号
     * @return Strip，不存在时为空
     */
    Optional<Strip> findByCode(String stripCode);

    /**
     * 插入。
     *
     * @param strip Strip
     */
    void save(Strip strip);

    /**
     * 列出某个 Lot 的 Strip。
     *
     * @param lotId Lot
     * @return Strip 列表
     */
    List<Strip> listByLot(Long lotId);

    /**
     * 更新。切筋成型时改状态。
     *
     * @param strip 已加载的 Strip
     */
    void update(Strip strip);
}
