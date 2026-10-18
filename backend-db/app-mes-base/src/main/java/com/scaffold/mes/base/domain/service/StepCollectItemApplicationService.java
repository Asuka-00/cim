package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.mes.base.domain.repository.StepCollectItemRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StepCollectItemApplicationService 站点采集规格
 * <p>
 * 至少要有上限或下限。同一站点、采集项和产品不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepCollectItemApplicationService {

    private final StepCollectItemRepository repository;

    /**
     * 列出这个站点的规格。
     *
     * @param stepId 站点
     * @return 规格
     */
    public List<StepCollectItem> list(Long stepId) {
        return repository.listByStep(stepId);
    }

    /**
     * 保存规格。
     *
     * @param request 站点、采集项和上下限
     * @throws BusinessException 上下限都空，或已经有规格
     */
    @Transactional
    public void save(StepCollectItemSaveRequest request) {
        if (request.getLsl() == null && request.getUsl() == null) {
            throw new BusinessException(MesBaseErrorCodes.STEP_COLLECT_INVALID, "规格至少要有上限或下限");
        }
        boolean autoHold = Integer.valueOf(1).equals(request.getAutoHoldOnOos());
        if (autoHold && (request.getHoldReasonCode() == null || request.getHoldReasonCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.STEP_COLLECT_INVALID, "自动 Hold 必须填写原因码");
        }
        Long productId = request.getProductId() == null ? 0L : request.getProductId();
        if (repository.find(request.getStepId(), request.getItemId(), productId).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_COLLECT_INVALID, "这个站点与采集项已有规格");
        }
        StepCollectItem item = new StepCollectItem();
        item.setStepId(request.getStepId());
        item.setItemId(request.getItemId());
        item.setProductId(productId);
        item.setLsl(request.getLsl());
        item.setUsl(request.getUsl());
        item.setAutoHoldOnOos(autoHold ? 1 : 0);
        item.setHoldReasonCode(request.getHoldReasonCode());
        item.setIsRequired(Integer.valueOf(0).equals(request.getIsRequired()) ? 0 : 1);
        repository.save(item);
    }
}
