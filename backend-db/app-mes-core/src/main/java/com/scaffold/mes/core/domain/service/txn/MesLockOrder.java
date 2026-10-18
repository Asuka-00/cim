package com.scaffold.mes.core.domain.service.txn;

import java.util.Comparator;
import java.util.List;

/**
 * MesLockOrder 事务内加锁顺序
 * <p>
 * 工单 → 设备 → Lot → Wafer → 其他。同一类按 id 升序,避免互相死锁。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesLockOrder {

    private static final List<String> RANK = List.of("WORK_ORDER", "EQUIPMENT", "LOT", "WAFER", "OTHER");

    private MesLockOrder() {
    }

    /**
     * 按固定顺序排列本事务要锁的行。未知类别排在最后。
     *
     * @param targets 调用方列出的行,允许重复
     * @return 去重后的加锁顺序
     */
    public static List<MesLockTarget> sort(List<MesLockTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        return targets.stream()
                .distinct()
                .sorted(Comparator.comparingInt(MesLockOrder::rank).thenComparingLong(MesLockTarget::id))
                .toList();
    }

    private static int rank(MesLockTarget target) {
        int index = RANK.indexOf(target.kind());
        return index < 0 ? RANK.size() : index;
    }
}
