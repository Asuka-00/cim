package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.Ecn;

/**
 * EcnRepository 变更申请仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EcnRepository {

    /**
     * 按编号查询。
     *
     * @param ecnNo 编号
     * @return 申请，不存在时为空
     */
    Optional<Ecn> findByNo(String ecnNo);

    /**
     * 按主键查询。
     *
     * @param id 申请
     * @return 申请，不存在时为空
     */
    Optional<Ecn> findById(Long id);

    /**
     * 列出全部申请，按编号。
     *
     * @return 申请
     */
    List<Ecn> list();

    /**
     * 更新状态。
     *
     * @param ecn 申请
     */
    void update(Ecn ecn);

    /**
     * 插入。
     *
     * @param ecn 申请
     */
    void save(Ecn ecn);
}
