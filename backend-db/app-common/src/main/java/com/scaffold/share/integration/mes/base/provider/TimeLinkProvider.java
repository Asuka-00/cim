package com.scaffold.share.integration.mes.base.provider;

import java.util.List;
import java.util.Optional;

import com.scaffold.share.integration.mes.base.response.TimeLinkStart;

/**
 * TimeLinkProvider 时间链
 * <p>
 * 执行模块不能直接读 Route。出站时用它找出以当前序号为起点的约束。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface TimeLinkProvider {

    /**
     * 列出从指定序号出发的时间链。
     *
     * @param routeVersionId Route 版本
     * @param fromSeqNo 起点序号
     * @return 约束，没有时为空列表
     */
    List<TimeLinkStart> startingAt(Long routeVersionId, Integer fromSeqNo);

    /**
     * 按主键取这个 Route 版本上的一条时间链。
     *
     * @param routeVersionId Route 版本
     * @param timeLinkId 约束
     * @return 约束，不在这个版本上时为空
     */
    Optional<TimeLinkStart> find(Long routeVersionId, Long timeLinkId);
}
