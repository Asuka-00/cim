package com.scaffold.share.infra.mapper;

import java.util.Collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * BatchBaseMapper 支持批量插入的 Mapper 基接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，额外提供 insertBatchSomeColumn 方法，
 * 生成单条 INSERT INTO ... VALUES (...), (...), ... SQL，性能远优于逐条 insert。
 * <p>
 * 需配合 {@link com.scaffold.share.config.BatchSqlInjector} 使用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-25
 * @param <T> 实体类型
 */
public interface BatchBaseMapper<T> extends BaseMapper<T> {

    /**
     * 批量插入（单条 SQL）
     *
     * @param entityList 实体列表
     * @return 插入行数
     */
    int insertBatchSomeColumn(Collection<T> entityList);
}
