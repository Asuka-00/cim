package com.scaffold.share.integration.mes.base.provider;

import java.util.Optional;

import com.scaffold.share.integration.mes.base.response.ReworkEntry;
import com.scaffold.share.integration.mes.base.response.ReworkProgress;

/**
 * ReworkRouteProvider 返工分支查询
 * <p>
 * 由 app-mes-base 实现。执行模块用它判断能否进入，不直接读 Route 表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ReworkRouteProvider {

    /**
     * 读取返工分支和它的第一步。
     *
     * @param reworkId 返工分支
     * @return 分支，不存在时为空
     */
    Optional<ReworkEntry> find(Long reworkId);

    /**
     * 当前返工步骤出站后去哪里。
     *
     * @param reworkId 返工分支
     * @param currentReworkSeq 当前返工序号
     * @return 下一条返工步骤，或回到主流程。分支不存在时为空
     */
    Optional<ReworkProgress> nextAfter(Long reworkId, Integer currentReworkSeq);
}
