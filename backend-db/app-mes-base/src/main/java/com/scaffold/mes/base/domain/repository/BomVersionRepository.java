package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.BomVersion;

/**
 * BomVersionRepository BOM 版本仓储
 * <p>
 * 按当前租户读写 mes_bom_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BomVersionRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 版本，不存在时为空
     */
    Optional<BomVersion> findById(Long id);

    /**
     * 分页查询某个 BOM 的版本。
     *
     * @param bomId BOM
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 按版本号倒序的分页结果
     */
    PageDTO<BomVersion> page(Long bomId, int page, int pageSize);

    /**
     * 列出某个 BOM 当前生效的版本。
     *
     * @param bomId BOM
     * @return 状态为 ACTIVE 的版本
     */
    List<BomVersion> listActive(Long bomId);

    /**
     * 计算下一个版本号。
     *
     * @param bomId BOM
     * @return 现有最大版本号加 1，没有版本时为 1
     */
    int nextVersionNo(Long bomId);

    /**
     * 插入。
     *
     * @param version 版本
     */
    void save(BomVersion version);

    /**
     * 更新。
     *
     * @param version 已加载的版本
     */
    void update(BomVersion version);
}
