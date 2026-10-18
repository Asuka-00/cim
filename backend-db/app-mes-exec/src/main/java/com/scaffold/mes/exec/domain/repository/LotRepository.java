package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.Lot;

/**
 * LotRepository Lot 仓储
 * <p>
 * 按当前租户读写 mes_lot。开批余量用已开批数量的实时合计，不读工单上的异步汇总值。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotRepository {

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param lotNo Lot 号模糊条件，空表示不限
     * @param status 状态，空表示不限
     * @return 分页结果
     */
    PageDTO<Lot> page(int page, int pageSize, String lotNo, String status);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Lot，不存在时为空
     */
    Optional<Lot> findById(Long id);

    /**
     * 按 Lot 号查询。
     *
     * @param lotNo Lot 号
     * @return Lot，不存在时为空
     */
    Optional<Lot> findByNo(String lotNo);

    /**
     * 合计某工单已开批数量。
     *
     * @param woId 工单
     * @return initial_qty 之和
     */
    long sumInitialQty(Long woId);

    /**
     * 列出某个工单下的全部 Lot。
     *
     * @param woId 工单
     * @return Lot 列表
     */
    java.util.List<Lot> listByWorkOrder(Long woId);

    /**
     * 插入。
     *
     * @param lot Lot
     */
    void save(Lot lot);

    /**
     * 更新已加载的 Lot。
     *
     * @param lot Lot
     */
    void update(Lot lot);

    /**
     * 对 Lot 行加 FOR UPDATE NOWAIT。
     *
     * @param id 主键
     */
    void lock(Long id);

    /**
     * 统计设备上正在加工的 Lot 数。
     *
     * @param equipmentId 设备
     * @return RUNNING 的批数
     */
    long countRunning(Long equipmentId);

    /**
     * 统计设备上的在制占用。同一批次算 1 个名额。excludeBatchId 非空时不计入该批次，供批次内后续 Lot 复用第一次校验。
     *
     * @param equipmentId 设备
     * @param excludeBatchId 不计入的批次，单独进站时为空
     * @return 占用名额
     */
    long countOccupancy(Long equipmentId, Long excludeBatchId);

    /**
     * 父 Lot 下已经用过的最大拆批序号。
     *
     * @param parentLotId 父 Lot
     * @return 没有子 Lot 时为 0
     */
    int maxSplitSeq(Long parentLotId);

    /**
     * 列出待加工且未 Hold 的 Lot。
     *
     * @param stepId 站点，空表示不限
     * @param areaId 区域，空表示不限
     * @return WAITING 且 hold_count 为 0 的 Lot
     */
    List<Lot> listWaiting(Long stepId, Long areaId);

    /**
     * 统计某个 Port 上正在加工的 Lot 数。
     *
     * @param portId Port 或腔体
     * @return RUNNING 的批数
     */
    long countRunningOnPort(Long portId);

    /**
     * 列出在这段时间内完成的 Lot。
     *
     * @param from 开始，含
     * @param to 结束，不含
     * @return 状态为 COMPLETED 且完成时间落在区间内的 Lot
     */
    List<Lot> listCompletedBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /**
     * 列出还在制的 Lot。
     *
     * @return 状态为 WAITING 或 RUNNING 的 Lot
     */
    List<Lot> listInProcess();
}
