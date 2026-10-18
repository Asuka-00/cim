package com.scaffold.mes.base.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ConfirmRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.repository.ConfirmRuleRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ConfirmRuleApplicationService 二次确认规则
 * <p>
 * 没有规则或已停用时返回 NONE。第二人签名必须指定角色。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ConfirmRuleApplicationService {

    private static final Set<String> MODES = Set.of("REAUTH", "SECOND_SIGN");

    private final ConfirmRuleRepository repository;

    /**
     * 列出全部规则。
     *
     * @return 按动作编码排列
     */
    public List<ConfirmRule> list() {
        return repository.list();
    }

    /**
     * 新建规则。
     *
     * @param request 动作和确认方式
     * @throws BusinessException 模式不合法、第二人签名没写角色，或动作已有规则
     */
    @Transactional
    public void create(ConfirmRuleCreateRequest request) {
        if (!MODES.contains(request.getConfirmMode())) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_RULE_INVALID, "确认方式不合法");
        }
        if ("SECOND_SIGN".equals(request.getConfirmMode())
                && (request.getSecondRoleCode() == null || request.getSecondRoleCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_RULE_INVALID, "第二人签名必须指定角色");
        }
        if (repository.findByAction(request.getActionCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.CONFIRM_RULE_INVALID, "这个动作已有确认规则");
        }
        ConfirmRule rule = new ConfirmRule();
        rule.setActionCode(request.getActionCode());
        rule.setConfirmMode(request.getConfirmMode());
        rule.setSecondRoleCode(request.getSecondRoleCode());
        rule.setEnabled(1);
        repository.save(rule);
    }

    /**
     * 这个动作当前需要的确认方式。
     *
     * @param actionCode 动作
     * @return NONE、REAUTH 或 SECOND_SIGN
     */
    public String required(String actionCode) {
        ConfirmRule rule = repository.findByAction(actionCode).orElse(null);
        if (rule == null || rule.getEnabled() == null || rule.getEnabled() != 1) {
            return "NONE";
        }
        return rule.getConfirmMode();
    }
}
