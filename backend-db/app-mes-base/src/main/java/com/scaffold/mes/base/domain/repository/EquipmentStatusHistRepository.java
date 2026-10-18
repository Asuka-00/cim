package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;

/**
 * EquipmentStatusHistRepository 设备状态历史仓储
 * <p>
 * 按当前租户读写 hist_mes_equipment_status。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentStatusHistRepository {

    /**
     * 查出某设备某个 Port 上尚未结束的时间段。
     *
     * @param equipmentId 设备
     * @param portId Port，0 表示设备级
     * @return 进行中的时间段，按开始时间从早到晚
     */
    List<EquipmentStatusHist> listOpen(Long equipmentId, Long portId);

    /**
     * 分页查询某设备的状态历史。
     *
     * @param equipmentId 设备
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 按开始时间倒序的分页结果
     */
    PageDTO<EquipmentStatusHist> page(Long equipmentId, int page, int pageSize);

    /**
     * 插入时间段。
     *
     * @param hist 时间段
     */
    void save(EquipmentStatusHist hist);

    /**
     * 回填结束时间和时长。
     *
     * @param hist 已加载的时间段
     */
    void update(EquipmentStatusHist hist);

    /**
     * 删掉尚未切换过的初始时间段。
     *
     * @param equipmentId 设备
     */
    void deleteUntouchedInitial(Long equipmentId);
}
