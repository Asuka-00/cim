package com.scaffold.mes.exec.domain.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.DispatchRuleRequest;
import com.scaffold.mes.exec.domain.entity.DispatchRule;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.DispatchRuleRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * DispatchRuleApplicationService 作业清单排序
 * <p>
 * 没有指定规则时用默认规则。没有默认规则时按优先级、再按进站时间。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class DispatchRuleApplicationService {

    private final DispatchRuleRepository repository;

    /**
     * 列出全部规则。
     *
     * @return 按编码排列
     */
    public List<DispatchRule> list() {
        return repository.list();
    }

    /**
     * 新建规则。
     *
     * @param request 编码和排序键
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(DispatchRuleRequest request) {
        if (repository.findByCode(request.getRuleCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.DISPATCH_RULE_CODE_EXISTS, "规则编码已存在");
        }
        DispatchRule rule = new DispatchRule();
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setScopeType("GLOBAL");
        rule.setScopeId(0L);
        rule.setSortKeys(request.getSortKeys());
        rule.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        rule.setEnabled(1);
        repository.save(rule);
    }

    /**
     * 按规则生成比较器。
     *
     * @param ruleId 指定规则，空则用默认规则
     * @return 比较器
     */
    public Comparator<Lot> comparator(Long ruleId) {
        DispatchRule rule = ruleId == null ? repository.findDefault().orElse(null) : repository.findById(ruleId).orElse(null);
        if (rule == null || rule.getEnabled() == null || rule.getEnabled() != 1 || rule.getSortKeys() == null) {
            return fallback();
        }
        Comparator<Lot> comparator = null;
        for (String part : rule.getSortKeys().split(",")) {
            String[] keyDir = part.trim().split(":");
            if (keyDir.length != 2) {
                continue;
            }
            Comparator<Lot> next = key(keyDir[0].trim(), "DESC".equalsIgnoreCase(keyDir[1].trim()));
            if (next == null) {
                continue;
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? fallback() : comparator;
    }

    private static Comparator<Lot> fallback() {
        return Comparator.comparing(Lot::getPriority, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Lot::getStepEnterAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static Comparator<Lot> key(String key, boolean desc) {
        Comparator<Lot> comparator = switch (key) {
            case "IS_HOT" -> Comparator.comparing(lot -> lot.getIsHot() == null ? 0 : lot.getIsHot());
            case "PRIORITY" -> Comparator.comparing(Lot::getPriority, Comparator.nullsLast(Integer::compareTo));
            case "STEP_ENTER_AT" -> Comparator.comparing(Lot::getStepEnterAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
        if (comparator == null) {
            return null;
        }
        return desc ? comparator.reversed() : comparator;
    }
}
