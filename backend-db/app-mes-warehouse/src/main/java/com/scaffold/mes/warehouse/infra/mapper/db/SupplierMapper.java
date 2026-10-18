package com.scaffold.mes.warehouse.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * SupplierMapper 供应商 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface SupplierMapper extends BatchBaseMapper<Supplier> {
}
