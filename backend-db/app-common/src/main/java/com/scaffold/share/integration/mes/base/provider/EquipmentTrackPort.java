package com.scaffold.share.integration.mes.base.provider;

/**
 * EquipmentTrackPort 进站时的设备状态联动
 * <p>
 * 只在设备当前为 STANDBY，或状态来源已是 TRACK 时调用。停机和人工状态不调用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentTrackPort {

    /**
     * 把设备状态改成加工态，来源记为 TRACK。
     *
     * @param equipmentId 设备
     * @param e10State PRODUCTIVE 或 ENGINEERING
     */
    void onTrackIn(Long equipmentId, String e10State);

    /**
     * 出站后，仅当状态来源是 TRACK 且设备上没有其他在制 Lot 时回到 STANDBY。
     *
     * @param equipmentId 设备
     * @param otherRunningLots 本批离开后仍在该设备上的 RUNNING 批数
     */
    void releaseIfIdle(Long equipmentId, long otherRunningLots);
}
