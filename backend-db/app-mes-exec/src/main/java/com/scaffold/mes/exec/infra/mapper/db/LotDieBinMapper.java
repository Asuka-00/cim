package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotDieBinMapper Die Bin Mapper
 * <p>
 * 映射 mes_lot_die_bin。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotDieBinMapper extends BatchBaseMapper<LotDieBin> {
}
