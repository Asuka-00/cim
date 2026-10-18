package com.scaffold.system.domain.repository.system;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.system.DictType;
import com.scaffold.system.domain.query.system.DictTypeQuery;

/**
 * DictTypeRepository 仓储接口
 * <p>
 * 负责定义字典类型相关数据访问能力。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
public interface DictTypeRepository {

    /**
     * 根据ID查询字典类型
     */
    Optional<DictType> findById(Long id);

    /**
     * 根据字典类型编码查询字典类型
     */
    Optional<DictType> findByDictType(String dictType);

    /**
     * 查询所有字典类型
     */
    List<DictType> findAll();

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<DictType> findActiveAll();

    /**
     * 保存字典类型
     */
    void save(DictType entity);

    /**
     * 更新字典类型
     */
    void update(DictType entity);

    /**
     * 根据ID删除字典类型
     */
    void deleteById(Long id);

    /**
     * 分页查询字典类型
     */
    PageDTO<DictType> queryPage(int page, int pageSize, DictTypeQuery query);
}
