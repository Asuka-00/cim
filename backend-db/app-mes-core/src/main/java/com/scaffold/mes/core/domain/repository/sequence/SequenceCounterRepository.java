package com.scaffold.mes.core.domain.repository.sequence;

import com.scaffold.mes.core.domain.entity.sequence.SequenceCounter;

/**
 * SequenceCounterRepository 编号流水仓储
 * <p>
 * 取号时锁定计数行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface SequenceCounterRepository {

    /**
     * 锁定当前租户下指定规则与周期的计数行。
     *
     * @param ruleId 规则主键
     * @param periodKey 周期键,不重置时为 -
     * @return 计数行,不存在时为 null
     */
    SequenceCounter lock(Long ruleId, String periodKey);

    /**
     * 插入记录。
     *
     * @param counter 待插入对象
     */
    void save(SequenceCounter counter);

    /**
     * 更新记录。
     *
     * @param counter 已加载的对象
     */
    void update(SequenceCounter counter);
}
