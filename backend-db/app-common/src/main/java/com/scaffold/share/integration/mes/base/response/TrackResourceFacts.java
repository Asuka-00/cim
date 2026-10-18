package com.scaffold.share.integration.mes.base.response;

/**
 * TrackResourceFacts 进站时设备与 Recipe 的判定材料
 * <p>
 * 由 app-mes-base 按当前主数据填好。执行模块用它跑设备、容量和 Recipe 规则，不直接读主数据表。
 *
 * @param needEquipment 站点是否要求设备
 * @param needRecipe 站点是否要求 Recipe
 * @param equipmentAllowed 设备是否允许在该站点加工
 * @param equipmentState 设备当前 E10。没有设备时为空
 * @param statusSource 状态来源 MANUAL、TRACK 或 SYSTEM
 * @param equipmentCapacity 设备同时加工上限
 * @param portCapacity Port 上限。没有指定 Port 时为空
 * @param recipeAllowed Recipe 版本是否可用
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record TrackResourceFacts(
        boolean needEquipment,
        boolean needRecipe,
        boolean equipmentAllowed,
        String equipmentState,
        String statusSource,
        int equipmentCapacity,
        Integer portCapacity,
        boolean recipeAllowed) {
}
