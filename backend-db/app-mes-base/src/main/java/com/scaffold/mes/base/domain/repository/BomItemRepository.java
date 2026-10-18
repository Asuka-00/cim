package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.BomItem;

/**
 * BomItemRepository BOM 行仓储
 * <p>
 * 按当前租户读写 mes_bom_item。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BomItemRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return BOM 行，不存在时为空
     */
    Optional<BomItem> findById(Long id);

    /**
     * 按版本和行号查询。
     *
     * @param bomVersionId BOM 版本
     * @param lineNo 行号
     * @return BOM 行，不存在时为空
     */
    Optional<BomItem> findByLine(Long bomVersionId, Integer lineNo);

    /**
     * 列出某个版本的全部行，按行号从小到大。
     *
     * @param bomVersionId BOM 版本
     * @return BOM 行
     */
    List<BomItem> listByVersion(Long bomVersionId);

    /**
     * 插入。
     *
     * @param item BOM 行
     */
    void save(BomItem item);

    /**
     * 更新。
     *
     * @param item 已加载的 BOM 行
     */
    void update(BomItem item);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
