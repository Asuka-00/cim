package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepSamplingBindRequest;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.mes.base.domain.repository.StepSamplingRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StepSamplingApplicationService 站点抽检绑定
 * <p>
 * 只接受已启用的规则。同一站点与产品已有绑定时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepSamplingApplicationService {

    private final SamplingRuleRepository rules;
    private final StepSamplingRepository bindings;

    /**
     * 列出全部绑定。
     *
     * @return 按站点排列
     */
    public List<StepSampling> list() {
        return bindings.list();
    }

    /**
     * 把规则绑到量测站点。
     *
     * @param request 站点、产品和规则
     * @throws BusinessException 规则不存在或已停用，或该站点与产品已绑定
     */
    @Transactional
    public void bind(StepSamplingBindRequest request) {
        SamplingRule rule = rules.findById(request.getRuleId()).orElse(null);
        if (rule == null || rule.getEnabled() == null || rule.getEnabled() != 1) {
            throw new BusinessException(MesBaseErrorCodes.STEP_SAMPLING_INVALID, "抽检规则不可用");
        }
        Long productId = request.getProductId() == null ? 0L : request.getProductId();
        if (bindings.find(request.getStepId(), productId).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_SAMPLING_INVALID, "这个站点与产品已绑定抽检规则");
        }
        StepSampling binding = new StepSampling();
        binding.setStepId(request.getStepId());
        binding.setProductId(productId);
        binding.setRuleId(request.getRuleId());
        bindings.save(binding);
    }
}
