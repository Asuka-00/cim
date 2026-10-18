package com.scaffold.mes.core.domain.service.sequence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.sequence.SequenceRuleSaveRequest;
import com.scaffold.mes.core.domain.entity.sequence.SequenceCounter;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.mes.core.domain.repository.sequence.SequenceCounterRepository;
import com.scaffold.mes.core.domain.repository.sequence.SequenceRuleRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * SequenceRuleApplicationService 编号规则应用服务
 * <p>
 * 维护规则,并在独立事务中取号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
public class SequenceRuleApplicationService {

    private final SequenceRuleRepository ruleRepository;
    private final SequenceCounterRepository counterRepository;
    private final TransactionTemplate nestedInsert;

    /**
     * 创建编号规则服务。
     *
     * @param ruleRepository 编号规则仓储
     * @param counterRepository 流水计数仓储
     * @param transactionManager 用于在取号事务内以保存点插入计数行,避免唯一键冲突把整笔取号标成回滚
     */
    public SequenceRuleApplicationService(SequenceRuleRepository ruleRepository,
            SequenceCounterRepository counterRepository, PlatformTransactionManager transactionManager) {
        this.ruleRepository = ruleRepository;
        this.counterRepository = counterRepository;
        this.nestedInsert = new TransactionTemplate(transactionManager);
        this.nestedInsert.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    /**
     * 分页查询编号规则。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param ruleCode 编码模糊条件,空表示不限
     * @return 分页结果
     */
    public PageDTO<SequenceRule> page(int page, int pageSize, String ruleCode) {
        return ruleRepository.page(page, pageSize, ruleCode);
    }

    /**
     * 按主键取得编号规则。
     *
     * @param id 规则主键
     * @return 规则
     * @throws BusinessException 规则不存在
     */
    public SequenceRule get(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.SEQUENCE_RULE_NOT_FOUND, "编号规则不存在"));
    }

    /**
     * 新增编号规则。
     *
     * @param request 规则内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(SequenceRuleSaveRequest request) {
        if (ruleRepository.findByRuleCode(request.getRuleCode()).isPresent()) {
            throw new BusinessException(MesErrorCodes.SEQUENCE_RULE_EXISTS, "编号规则已存在: " + request.getRuleCode());
        }
        SequenceRule rule = new SequenceRule();
        copy(request, rule);
        ruleRepository.save(rule);
    }

    /**
     * 修改编号规则。
     *
     * @param id 规则主键
     * @param request 规则内容
     * @throws BusinessException 规则不存在
     */
    @Transactional
    public void update(Long id, SequenceRuleSaveRequest request) {
        SequenceRule rule = get(id);
        copy(request, rule);
        ruleRepository.update(rule);
    }

    /**
     * 删除编号规则。
     *
     * @param id 规则主键
     * @throws BusinessException 规则不存在
     */
    @Transactional
    public void delete(Long id) {
        get(id);
        ruleRepository.deleteById(id);
    }

    /**
     * 预览下一个号码,不消耗流水。
     *
     * @param ruleCode 规则编码
     * @return 预览号码
     * @throws BusinessException 规则不存在或已停用
     */
    public String preview(String ruleCode) {
        SequenceRule rule = requireRule(ruleCode);
        LocalDate today = LocalDate.now();
        SequenceCounter counter = counterRepository.lock(rule.getId(), SequenceNumbers.periodKey(rule.getResetCycle(), today));
        long current = counter == null || counter.getCurrentValue() == null ? 0L : counter.getCurrentValue();
        long start = rule.getSeqStart() == null ? 1L : rule.getSeqStart();
        int step = rule.getSeqStep() == null ? 1 : rule.getSeqStep();
        return SequenceNumbers.format(rule, SequenceNumbers.nextValue(current, start, step), today);
    }

    /**
     * 取号。独立短事务,业务回滚不回退流水。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String ruleCode) {
        SequenceRule rule = requireRule(ruleCode);
        LocalDate today = LocalDate.now();
        String period = SequenceNumbers.periodKey(rule.getResetCycle(), today);
        SequenceCounter counter = counterRepository.lock(rule.getId(), period);
        if (counter == null) {
            SequenceCounter created = new SequenceCounter();
            created.setRuleId(rule.getId());
            created.setPeriodKey(period);
            created.setCurrentValue(0L);
            try {
                nestedInsert.executeWithoutResult(status -> counterRepository.save(created));
            } catch (DuplicateKeyException ex) {
                // 并发插入已落库,保存点已回滚,下面重新加锁
            }
            counter = counterRepository.lock(rule.getId(), period);
        }
        long current = counter.getCurrentValue() == null ? 0L : counter.getCurrentValue();
        long start = rule.getSeqStart() == null ? 1L : rule.getSeqStart();
        int step = rule.getSeqStep() == null ? 1 : rule.getSeqStep();
        long value = SequenceNumbers.nextValue(current, start, step);
        String number = SequenceNumbers.format(rule, value, today);
        counter.setCurrentValue(value);
        counter.setUpdateAt(LocalDateTime.now());
        counterRepository.update(counter);
        return number;
    }

    private SequenceRule requireRule(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .filter(rule -> rule.getEnabled() == null || rule.getEnabled() == 1)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.SEQUENCE_RULE_NOT_FOUND, "编号规则不存在或已停用: " + ruleCode));
    }

    private static void copy(SequenceRuleSaveRequest request, SequenceRule rule) {
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setBizType(request.getBizType());
        rule.setPrefix(request.getPrefix());
        rule.setDatePattern(request.getDatePattern());
        rule.setSeqLength(request.getSeqLength());
        rule.setSeqStart(request.getSeqStart());
        rule.setSeqStep(request.getSeqStep());
        rule.setResetCycle(request.getResetCycle());
        rule.setSuffix(request.getSuffix());
        rule.setRemark(request.getRemark());
        rule.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
