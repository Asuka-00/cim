package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ProductUnitConvMapper 产品计数换算 Mapper
 * <p>
 * 映射 mes_product_unit_conv。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ProductUnitConvMapper extends BatchBaseMapper<ProductUnitConv> {
}
