package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.BatchCreateRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BatchApplicationService 组批
 * <p>
 * 只接收同站点、等待中且未 Hold 的 Lot。达到最小批量后状态变为 READY。进站前可以移出。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BatchApplicationService {

    private final BatchRepository batchRepository;
    private final BatchLotRepository memberRepository;
    private final LotRepository lotRepository;
    private final Clock clock;

    /**
     * 列出批次。
     *
     * @return 按批次号排列
     */
    public List<Batch> list() {
        return batchRepository.list();
    }

    /**
     * 新建空批次。
     *
     * @param request 批次号、站点和容量
     * @throws BusinessException 批次号重复或容量小于 1
     */
    @Transactional
    public void create(BatchCreateRequest request) {
        int minLots = request.getMinLots() == null ? 1 : request.getMinLots();
        if (request.getMaxLots() == null || request.getMaxLots() < 1 || minLots < 1 || minLots > request.getMaxLots()) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "批次容量至少为 1，且下限不能大于上限");
        }
        if (batchRepository.findByNo(request.getBatchNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.BATCH_NO_EXISTS, "批次号已存在");
        }
        Batch batch = new Batch();
        batch.setBatchNo(request.getBatchNo());
        batch.setStepId(request.getStepId());
        batch.setStatus("OPEN");
        batch.setMinLots(minLots);
        batch.setMaxLots(request.getMaxLots());
        batch.setLotCount(0);
        batch.setWaferCount(0);
        batch.setTotalQty(0L);
        batch.setVersion(0);
        batchRepository.save(batch);
    }

    /**
     * 把 Lot 加入批次。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @throws BusinessException Lot 未就绪、站点不同、已在其他批次，或批次已满
     */
    @Transactional
    public void add(Long batchId, Long lotId) {
        Batch batch = requireOpen(batchId);
        if (batch.getLotCount() >= batch.getMaxLots()) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "批次已满");
        }
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus()) || (lot.getHoldCount() != null && lot.getHoldCount() > 0)) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_NOT_READY, "只有未 Hold 的等待 Lot 可以加入批次");
        }
        if (!batch.getStepId().equals(lot.getStepId())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_STEP_MISMATCH, "Lot 不在该批次的站点");
        }
        if (lot.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "Lot 已经在批次里");
        }
        if (batch.getRecipeVersionId() == null) {
            batch.setRecipeVersionId(lot.getRecipeVersionId());
        } else if (!batch.getRecipeVersionId().equals(lot.getRecipeVersionId())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_RECIPE_MISMATCH, "Recipe 版本与批次不一致");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BatchLot member = memberRepository.find(batchId, lotId).orElse(null);
        if (member == null) {
            member = new BatchLot();
            member.setBatchId(batchId);
            member.setLotId(lotId);
            member.setSeq(batch.getLotCount() + 1);
            member.setQty(lot.getQty());
            member.setStatus("IN");
            member.setAddedAt(now);
            memberRepository.save(member);
        } else {
            member.setStatus("IN");
            member.setQty(lot.getQty());
            member.setAddedAt(now);
            member.setRemovedAt(null);
            memberRepository.update(member);
        }
        lot.setBatchId(batchId);
        lotRepository.update(lot);
        batch.setLotCount(batch.getLotCount() + 1);
        batch.setWaferCount(batch.getWaferCount() + (lot.getWaferCount() == null ? 0 : lot.getWaferCount()));
        batch.setTotalQty(batch.getTotalQty() + (lot.getQty() == null ? 0 : lot.getQty()));
        batch.setStatus(batch.getLotCount() >= minimum(batch) ? "READY" : "OPEN");
        batchRepository.update(batch);
    }

    /**
     * 进站前移出 Lot。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @throws BusinessException 批次已进站或成员不在批内
     */
    @Transactional
    public void remove(Long batchId, Long lotId) {
        Batch batch = requireOpen(batchId);
        BatchLot member = memberRepository.find(batchId, lotId)
                .filter(row -> "IN".equals(row.getStatus()))
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "Lot 不在该批次里"));
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        member.setStatus("REMOVED");
        member.setRemovedAt(LocalDateTime.now(clock));
        memberRepository.update(member);
        lot.setBatchId(null);
        lotRepository.update(lot);
        batch.setLotCount(batch.getLotCount() - 1);
        batch.setWaferCount(Math.max(0, batch.getWaferCount() - (lot.getWaferCount() == null ? 0 : lot.getWaferCount())));
        batch.setTotalQty(Math.max(0, batch.getTotalQty() - (lot.getQty() == null ? 0 : lot.getQty())));
        batch.setStatus(batch.getLotCount() >= minimum(batch) ? "READY" : "OPEN");
        batchRepository.update(batch);
    }

    /**
     * 取消尚未进站的批次。成员全部移出，Lot 上的批次号清空。
     *
     * @param batchId 批次
     * @throws BusinessException 批次已进站或已结束
     */
    @Transactional
    public void cancel(Long batchId) {
        Batch batch = requireOpen(batchId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (BatchLot member : memberRepository.listIn(batchId)) {
            Lot lot = lotRepository.findById(member.getLotId())
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
            member.setStatus("REMOVED");
            member.setRemovedAt(now);
            memberRepository.update(member);
            lot.setBatchId(null);
            lotRepository.update(lot);
        }
        batch.setLotCount(0);
        batch.setWaferCount(0);
        batch.setTotalQty(0L);
        batch.setStatus("CANCELLED");
        batchRepository.update(batch);
    }

    private static int minimum(Batch batch) {
        return batch.getMinLots() == null ? 1 : batch.getMinLots();
    }

    private Batch requireOpen(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        if (!"OPEN".equals(batch.getStatus()) && !"READY".equals(batch.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "当前状态不能改批次成员");
        }
        return batch;
    }

    /**
     * 列出仍在批内的成员。
     *
     * @param batchId 批次
     * @return 按序号排列
     */
    public List<BatchLot> members(Long batchId) {
        return memberRepository.listIn(batchId);
    }
}
