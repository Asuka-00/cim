package com.scaffold.system.infra.mapper.db.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.share.infra.mapper.BatchBaseMapper;
import com.scaffold.system.domain.entity.system.DictItem;

/**
 * DictItemMapper 映射器接口
 * <p>
 * 负责 sys_dict_item 的基础持久化和启动初始化分页查询。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@Mapper
public interface DictItemMapper extends BatchBaseMapper<DictItem> {

    /**
     * 初始化时忽略租户过滤分页查询字典选项
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_dict_item")
    IPage<DictItem> selectInitPageIgnoreTenant(IPage<DictItem> page);
}
