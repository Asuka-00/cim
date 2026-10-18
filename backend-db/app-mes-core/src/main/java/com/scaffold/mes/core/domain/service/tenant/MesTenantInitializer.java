package com.scaffold.mes.core.domain.service.tenant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.mes.core.domain.repository.sequence.SequenceRuleRepository;
import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.integration.system.event.TenantCreatedEvent;
import com.scaffold.share.integration.system.provider.MesTenantSeedProvider;

import lombok.RequiredArgsConstructor;

/**
 * MesTenantInitializer 租户 MES 初始化
 * <p>
 * 新建租户或手工触发时,幂等写入编号规则,并委托系统模块写入字典、参数和角色授权。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MesTenantInitializer {

    private final MesProperties properties;
    private final SequenceRuleRepository ruleRepository;
    private final MesTenantSeedProvider mesTenantSeedProvider;

    /**
     * 新建租户后初始化该租户的 MES 种子。
     *
     * @param event 租户已创建事件
     */
    @EventListener
    @Transactional
    public void onTenantCreated(TenantCreatedEvent event) {
        init(event.tenantId());
    }

    /**
     * 为指定租户幂等写入编号规则、字典、参数和角色授权。
     *
     * @param tenantId 租户标识
     */
    @Transactional
    public void init(String tenantId) {
        if (!properties.isEnabled()) {
            return;
        }
        TenantContextHolder.runAs(tenantId, () -> {
            saveIfAbsent("WORK_ORDER", "工单号", "WORK_ORDER", "WO", "yyyyMMdd", 4, "DAY");
            saveIfAbsent("LOT", "Lot 号", "LOT", "L", "yyMM", 5, "MONTH");
        });
        mesTenantSeedProvider.seed(tenantId);
    }

    private void saveIfAbsent(String code, String name, String bizType, String prefix, String datePattern,
            int seqLength, String resetCycle) {
        if (ruleRepository.findByRuleCode(code).isPresent()) {
            return;
        }
        SequenceRule rule = new SequenceRule();
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setBizType(bizType);
        rule.setPrefix(prefix);
        rule.setDatePattern(datePattern);
        rule.setSeqLength(seqLength);
        rule.setSeqStart(1L);
        rule.setSeqStep(1);
        rule.setResetCycle(resetCycle);
        rule.setEnabled(1);
        ruleRepository.save(rule);
    }
}
