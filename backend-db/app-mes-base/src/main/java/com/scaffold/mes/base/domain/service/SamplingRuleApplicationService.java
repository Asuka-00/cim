package com.scaffold.mes.base.domain.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.SamplingRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * SamplingRuleApplicationService 抽检规则
 * <p>
 * 可以按间隔批数，或按百分比。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class SamplingRuleApplicationService {

    private final SamplingRuleRepository repository;

    /**
     * 列出全部规则。
     *
     * @return 按编码排列
     */
    public List<SamplingRule> list() {
        return repository.list();
    }

    /**
     * 新建按间隔批数的规则。
     *
     * @param request 编码、名称和间隔
     * @throws BusinessException 间隔小于 1，或编码已存在
     */
    @Transactional
    public void create(SamplingRuleCreateRequest request) {
        String type = request.getSampleType() == null || request.getSampleType().isBlank()
                ? "EVERY_N_LOT" : request.getSampleType();
        if ("EVERY_N_LOT".equals(type) && (request.getNValue() == null || request.getNValue() < 1)) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "抽检间隔至少为 1");
        }
        if ("PERCENT".equals(type) && !validPercent(request.getPercent())) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "抽检百分比要在 0 和 100 之间");
        }
        if (request.getWaferCount() != null && request.getWaferCount() < 1) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "抽取片数至少为 1");
        }
        if (!validSlots(request.getSlotList())) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "槽位必须是逗号分隔的正整数");
        }
        if (!"EVERY_N_LOT".equals(type) && !"PERCENT".equals(type) && !"ALL".equals(type)) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "抽检方式不合法");
        }
        if (repository.findByCode(request.getRuleCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.SAMPLING_RULE_INVALID, "抽检规则编码已存在");
        }
        SamplingRule rule = new SamplingRule();
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setSampleType(type);
        rule.setNValue(request.getNValue());
        rule.setPercent(request.getPercent());
        rule.setWaferCount(request.getWaferCount());
        rule.setSlotList(request.getSlotList() == null || request.getSlotList().isBlank() ? null : request.getSlotList().trim());
        rule.setEnabled(1);
        repository.save(rule);
    }

    private static boolean validSlots(String slots) {
        if (slots == null || slots.isBlank()) {
            return true;
        }
        for (String part : slots.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) < 1) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }

    private static boolean validPercent(BigDecimal percent) {
        return percent != null && percent.signum() > 0 && percent.compareTo(new BigDecimal("100")) <= 0;
    }
}
