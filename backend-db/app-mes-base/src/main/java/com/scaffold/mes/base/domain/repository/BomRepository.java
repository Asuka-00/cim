package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Bom;

/**
 * BomRepository BOM 仓储
 * <p>
 * 按当前租户读写 mes_bom。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BomRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return BOM，不存在时为空
     */
    Optional<Bom> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param bomCode BOM 编码
     * @return BOM，不存在时为空
     */
    Optional<Bom> findByCode(String bomCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param bomCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Bom> page(int page, int pageSize, String bomCode);

    /**
     * 插入。
     *
     * @param bom BOM
     */
    void save(Bom bom);

    /**
     * 更新。
     *
     * @param bom 已加载的 BOM
     */
    void update(Bom bom);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计版本引用数。产品版本绑定 bom_id 后要补进这条统计。
     *
     * @param id BOM 主键
     * @return 引用条数
     */
    long countReferences(Long id);

    /**
     * 锁住 BOM 主表行，供版本生效串行化。
     *
     * @param id BOM 主键
     * @throws com.scaffold.share.exception.BusinessException BOM 不存在或行被占用
     */
    void lock(Long id);
}
