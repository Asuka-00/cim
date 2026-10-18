package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EsignRequest;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.entity.Esign;
import com.scaffold.mes.base.domain.repository.ConfirmRuleRepository;
import com.scaffold.mes.base.domain.repository.EsignRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * EsignApplicationService 电子签名
 * <p>
 * 只有启用的确认规则才写签名。再次确认由当前用户签。第二人签名必须是另一个用户。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EsignApplicationService {

    private final ConfirmRuleRepository rules;
    private final EsignRepository repository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 为一次需要确认的动作签名。
     *
     * @param request 动作、对象和签名人
     * @throws BusinessException 不需要签名，或签名人不符合方式
     */
    @Transactional
    public void sign(EsignRequest request) {
        ConfirmRule rule = rules.findByAction(request.getActionCode())
                .filter(row -> row.getEnabled() != null && row.getEnabled() == 1)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ESIGN_NOT_REQUIRED, "这个动作不需要签名"));
        String current = session.getCurrentUserNameOrDefault();
        String signer;
        String signType;
        String role = null;
        if ("SECOND_SIGN".equals(rule.getConfirmMode())) {
            signer = request.getSigner();
            if (signer == null || signer.isBlank() || signer.equals(current)) {
                throw new BusinessException(MesBaseErrorCodes.ESIGN_SIGNER, "第二签名人必须是另一个用户");
            }
            signType = "SECOND";
            role = rule.getSecondRoleCode();
        } else if ("REAUTH".equals(rule.getConfirmMode())) {
            signer = current;
            signType = "PRIMARY";
        } else {
            throw new BusinessException(MesBaseErrorCodes.ESIGN_NOT_REQUIRED, "这个动作不需要签名");
        }
        Esign esign = new Esign();
        esign.setActionCode(request.getActionCode());
        esign.setBizType(request.getBizType());
        esign.setBizId(request.getBizId());
        esign.setSigner(signer);
        esign.setSignerRole(role);
        esign.setSignType(signType);
        esign.setSignedAt(LocalDateTime.now(clock));
        repository.save(esign);
    }
}
