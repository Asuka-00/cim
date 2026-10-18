package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * HoldReasonMapper Hold 原因 Mapper
 * <p>
 * 映射 mes_hold_reason，并统计 Lot Hold 对原因码的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface HoldReasonMapper extends BatchBaseMapper<HoldReason> {

    /**
     * 统计使用该原因码的 Lot Hold 行数。
     *
     * @param reasonCode 原因码
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_lot_hold WHERE reason_code = #{reasonCode}")
    long countHolds(@Param("reasonCode") String reasonCode);
}
