package com.scaffold.mes.core.domain.repository.print;

import java.util.List;

import com.scaffold.mes.core.domain.entity.print.PrintLog;

/**
 * PrintLogRepository 打印记录仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface PrintLogRepository {

    /**
     * 这个对象是否已经用过该模板打过。
     *
     * @param bizType 对象类型
     * @param bizId 对象
     * @param tplId 模板
     * @return 打过时为 true
     */
    boolean exists(String bizType, Long bizId, Long tplId);

    /**
     * 按对象列出打印记录，按打印时间从早到晚。
     *
     * @param bizType 对象类型
     * @param bizId 对象
     * @return 打印记录。没有时为空列表
     */
    List<PrintLog> listByBiz(String bizType, Long bizId);

    /**
     * 追加一条记录。
     *
     * @param log 打印记录
     */
    void save(PrintLog log);
}
