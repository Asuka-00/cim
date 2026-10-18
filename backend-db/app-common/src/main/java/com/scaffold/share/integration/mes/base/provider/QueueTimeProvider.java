package com.scaffold.share.integration.mes.base.provider;

import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;

/**
 * QueueTimeProvider 排队时间
 * <p>
 * 执行模块不能直接读 Route 步骤。没有上限时不拦截进站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface QueueTimeProvider {

    /**
     * 读取指定步骤的排队时间。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @return 上限和动作。步骤不存在时上限为空
     */
    QueueTimeLimit limit(Long routeVersionId, Integer seqNo);
}
