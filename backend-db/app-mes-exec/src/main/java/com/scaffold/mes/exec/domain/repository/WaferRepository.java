package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.Wafer;

/**
 * WaferRepository Wafer 仓储
 * <p>
 * 按当前租户读写 mes_wafer。内部编号在租户内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WaferRepository {

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param waferCode 内部编号模糊条件，空表示不限
     * @param lotId Lot，空表示不限
     * @return 分页结果
     */
    PageDTO<Wafer> page(int page, int pageSize, String waferCode, Long lotId);

    /**
     * 按内部编号查询。
     *
     * @param waferCode 本厂编号
     * @return Wafer，不存在时为空
     */
    Optional<Wafer> findByCode(String waferCode);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Wafer，不存在时为空
     */
    Optional<Wafer> findById(Long id);

    /**
     * 列出某 Lot 下的全部片。
     *
     * @param lotId Lot
     * @return 片列表
     */
    List<Wafer> listByLot(Long lotId);

    /**
     * 插入。
     *
     * @param wafer Wafer
     */
    void save(Wafer wafer);

    /**
     * 更新。
     *
     * @param wafer 已加载的片
     */
    void update(Wafer wafer);

    /**
     * 对片加 FOR UPDATE NOWAIT。
     *
     * @param id 主键
     */
    void lock(Long id);
}
