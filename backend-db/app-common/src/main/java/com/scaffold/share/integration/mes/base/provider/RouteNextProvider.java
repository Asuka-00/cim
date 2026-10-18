package com.scaffold.share.integration.mes.base.provider;

import java.util.Optional;

import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * RouteNextProvider 默认下一步
 * <p>
 * 由 app-mes-base 实现。没有更大的序号时返回空，出站把 Lot 置为完成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteNextProvider {

    /**
     * 取比当前序号更大的最小步骤。
     *
     * @param routeVersionId Route 版本
     * @param currentSeq 当前步骤序号
     * @return 下一步，已经是最后一步时为空
     */
    Optional<RouteStepNext> next(Long routeVersionId, Integer currentSeq);

    /**
     * 按序号取步骤。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @return 该序号的步骤，没有时为空
     */
    Optional<RouteStepNext> at(Long routeVersionId, Integer seqNo);

    /**
     * 取比当前序号更小的最大步骤。
     *
     * @param routeVersionId Route 版本
     * @param currentSeq 当前步骤序号
     * @return 上一步，已经是第一步时为空
     */
    Optional<RouteStepNext> previous(Long routeVersionId, Integer currentSeq);

    /**
     * 这个序号是不是抽检站点。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @return 标记为抽检站点时为 true
     */
    boolean sampling(Long routeVersionId, Integer seqNo);
}
