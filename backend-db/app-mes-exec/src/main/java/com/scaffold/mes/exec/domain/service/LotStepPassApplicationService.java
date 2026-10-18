package com.scaffold.mes.exec.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotStepPassApplicationService 站点经过次数
 * <p>
 * Track-Out 加 1，Undo 减 1。到达站点时的经过序号是已完成次数加 1，没有行时视为 0。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotStepPassApplicationService {

    private final LotStepPassRepository repository;

    /**
     * 到达站点时应写入 Lot 的 pass_no。
     *
     * @param lotId Lot
     * @param stepId 站点
     * @return 已 Track-Out 次数加 1
     */
    public int arrivalPassNo(Long lotId, Long stepId) {
        return repository.find(lotId, stepId).map(LotStepPass::getPassCount).orElse(0) + 1;
    }

    /**
     * 记录一次 Track-Out。没有行时插入，已有行时加 1。
     *
     * @param lotId Lot
     * @param stepId 站点
     */
    @Transactional
    public void recordTrackOut(Long lotId, Long stepId) {
        LotStepPass pass = repository.find(lotId, stepId).orElse(null);
        if (pass == null) {
            pass = new LotStepPass();
            pass.setLotId(lotId);
            pass.setStepId(stepId);
            pass.setPassCount(1);
            repository.save(pass);
            return;
        }
        pass.setPassCount(pass.getPassCount() + 1);
        repository.update(pass);
    }

    /**
     * 回退一次 Track-Out。次数为 0 或没有行时拒绝。
     *
     * @param lotId Lot
     * @param stepId 站点
     * @throws BusinessException 没有可回退的次数
     */
    @Transactional
    public void undoTrackOut(Long lotId, Long stepId) {
        LotStepPass pass = repository.find(lotId, stepId)
                .filter(row -> row.getPassCount() != null && row.getPassCount() > 0)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_PASS_NOTHING_TO_UNDO, "该站点没有可回退的 Track-Out"));
        pass.setPassCount(pass.getPassCount() - 1);
        repository.update(pass);
    }
}
