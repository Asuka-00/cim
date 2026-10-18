package com.scaffold.mes.warehouse.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.warehouse.domain.entity.ReceiptItem;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ReceiptItemMapper 收货明细 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ReceiptItemMapper extends BatchBaseMapper<ReceiptItem> {
}
