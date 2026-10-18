package com.scaffold.share.infra.repository.db;

import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.entity.AppendOnlyEntity;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * AppendOnlyRepositorySupport 只增表仓储基座
 * <p>
 * 只提供插入与查询,避免只增表走通用更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public class AppendOnlyRepositorySupport<T extends AppendOnlyEntity> {

    private final BatchBaseMapper<T> mapper;
    private final Class<T> entityClass;

    public AppendOnlyRepositorySupport(BatchBaseMapper<T> mapper, Class<T> entityClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
    }

    /**
     * 插入一条只增记录。
     *
     * @param entity 待插入实体
     */
    public void save(T entity) {
        mapper.insert(entity);
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 实体,不存在时为空
     */
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(id).map(mapper::selectById);
    }

    /**
     * 按条件查询一条。调用方不要再追加 LIMIT。
     *
     * @param wrapper 查询条件
     * @return 实体,不存在时为空
     */
    public Optional<T> findOne(LambdaQueryWrapper<T> wrapper) {
        return Optional.ofNullable(mapper.selectOne(wrapper.last("LIMIT 1")));
    }

    /**
     * 分页查询。页码与页大小小于 1 时按 1 处理。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param wrapper 查询条件
     * @return 分页结果
     */
    public PageDTO<T> queryPage(int page, int pageSize, LambdaQueryWrapper<T> wrapper) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        Page<T> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        PageDTO<T> pageDTO = new PageDTO<>();
        pageDTO.setRecords(result.getRecords());
        pageDTO.setTotal(result.getTotal());
        pageDTO.setSize(result.getSize());
        pageDTO.setCurrent(result.getCurrent());
        return pageDTO;
    }

    /**
     * 创建按创建时间倒序的查询条件。
     *
     * @return 查询条件
     */
    public LambdaQueryWrapper<T> wrapper() {
        return new LambdaQueryWrapper<>(entityClass).orderByDesc(T::getCreateAt);
    }
}
