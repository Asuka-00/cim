package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotMapper Lot Mapper
 * <p>
 * 映射 mes_lot，并合计工单已开批数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotMapper extends BatchBaseMapper<Lot> {

    /**
     * 合计工单下 Lot 的开批数量。
     *
     * @param woId 工单
     * @return 合计，没有 Lot 时为 0
     */
    @Select("SELECT COALESCE(SUM(initial_qty), 0) FROM mes_lot WHERE wo_id = #{woId}")
    long sumInitialQty(@Param("woId") Long woId);

    /**
     * 统计设备上 RUNNING 的 Lot。
     *
     * @param equipmentId 设备
     * @return 批数
     */
    @Select("SELECT COUNT(1) FROM mes_lot WHERE equipment_id = #{equipmentId} AND status = 'RUNNING'")
    long countRunning(@Param("equipmentId") Long equipmentId);

    /**
     * 统计设备在制占用。同一 batch_id 只算 1。excludeBatchId 非空时跳过该批次。
     *
     * @param equipmentId 设备
     * @param excludeBatchId 不计入的批次，可空
     * @return 占用名额
     */
    @Select("""
            SELECT COUNT(DISTINCT COALESCE(batch_id, id)) FROM mes_lot
            WHERE equipment_id = #{equipmentId} AND status = 'RUNNING'
            AND (#{excludeBatchId} IS NULL OR batch_id IS NULL OR batch_id <> #{excludeBatchId})
            """)
    long countOccupancy(@Param("equipmentId") Long equipmentId, @Param("excludeBatchId") Long excludeBatchId);

    /**
     * 统计 Port 上 RUNNING 的 Lot。
     *
     * @param portId Port 或腔体
     * @return 批数
     */
    @Select("SELECT COUNT(1) FROM mes_lot WHERE port_id = #{portId} AND status = 'RUNNING'")
    long countRunningOnPort(@Param("portId") Long portId);

    /**
     * 父 Lot 下最大拆批序号。
     *
     * @param parentLotId 父 Lot
     * @return 没有子 Lot 时为 0
     */
    @Select("SELECT COALESCE(MAX(split_seq), 0) FROM mes_lot WHERE parent_lot_id = #{parentLotId}")
    int maxSplitSeq(@Param("parentLotId") Long parentLotId);
}
