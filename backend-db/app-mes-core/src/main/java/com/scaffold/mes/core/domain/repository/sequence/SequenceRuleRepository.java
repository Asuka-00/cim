package com.scaffold.mes.core.domain.repository.sequence;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;

/**
 * SequenceRuleRepository 编号规则仓储
 * <p>
 * 按当前租户读写 mes_sequence_rule。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface SequenceRuleRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 记录,不存在时为空
     */
    Optional<SequenceRule> findById(Long id);

    /**
     * 按规则编码查询当前租户的编号规则。
     *
     * @param ruleCode 规则编码
     * @return 规则,不存在时为空
     */
    Optional<SequenceRule> findByRuleCode(String ruleCode);

    /**
     * 分页查询编号规则。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param ruleCode 编码模糊条件,空表示不限
     * @return 分页结果
     */
    PageDTO<SequenceRule> page(int page, int pageSize, String ruleCode);

    /**
     * 插入记录。
     *
     * @param rule 待插入对象
     */
    void save(SequenceRule rule);

    /**
     * 更新记录。影响行数为 0 时按并发冲突处理。
     *
     * @param rule 已加载的对象
     * @throws com.scaffold.share.exception.BusinessException 记录不存在或已被他人修改
     */
    void update(SequenceRule rule);

    /**
     * 按主键物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
