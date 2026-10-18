package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.Handover;

/**
 * HandoverRepository 交接班仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface HandoverRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 交接班，不存在时为空
     */
    Optional<Handover> findById(Long id);

    /**
     * 按单号查询。
     *
     * @param handoverNo 交接单号
     * @return 交接班，不存在时为空
     */
    Optional<Handover> findByNo(String handoverNo);

    /**
     * 列出全部交接班，按单号。
     *
     * @return 交接班
     */
    List<Handover> list();

    /**
     * 插入。
     *
     * @param handover 交接班
     */
    void save(Handover handover);

    /**
     * 更新。
     *
     * @param handover 交接班
     */
    void update(Handover handover);
}
