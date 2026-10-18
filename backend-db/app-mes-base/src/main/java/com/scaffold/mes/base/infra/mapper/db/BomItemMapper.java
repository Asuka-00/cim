package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * BomItemMapper BOM 行 Mapper
 * <p>
 * 映射 mes_bom_item。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface BomItemMapper extends BatchBaseMapper<BomItem> {
}
