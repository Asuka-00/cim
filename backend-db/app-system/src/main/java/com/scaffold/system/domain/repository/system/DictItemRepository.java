package com.scaffold.system.domain.repository.system;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.system.DictItem;
import com.scaffold.system.domain.query.system.DictItemQuery;

/**
 * DictItemRepository 仓储接口
 * <p>
 * 负责定义字典选项相关数据访问能力。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
public interface DictItemRepository {

    /**
     * 根据ID查询字典选项
     */
    Optional<DictItem> findById(Long id);

    /**
     * 根据字典类型和选项编码查询字典选项
     */
    Optional<DictItem> findByDictTypeAndItemCode(String dictType, String itemCode);

    /**
     * 根据字典类型查询字典选项
     */
    List<DictItem> findByDictType(String dictType);

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<DictItem> findActiveAll();

    /**
     * 保存字典选项
     */
    void save(DictItem entity);

    /**
     * 更新字典选项
     */
    void update(DictItem entity);

    /**
     * 根据ID删除字典选项
     */
    void deleteById(Long id);

    /**
     * 分页查询字典选项
     */
    PageDTO<DictItem> queryPage(int page, int pageSize, DictItemQuery query);
}
