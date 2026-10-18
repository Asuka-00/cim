package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotHoldRequest;
import com.scaffold.mes.exec.domain.dto.LotReleaseRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;

import lombok.RequiredArgsConstructor;

/**
 * LotHoldApplicationService Lot Hold
 * <p>
 * 手工 Hold 每次新增一条，不写幂等键。系统 Hold 在持有 Lot 行锁后按 active_key 先查后插。Release 只处理一条，并减少 hold_count。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotHoldApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");
    private static final Set<String> SOURCES = Set.of(
            "MANUAL", "INTERLOCK", "OOS", "QUEUE_TIME", "TIME_LINK", "FUTURE_ACTION", "NCR", "SYSTEM");

    private final LotHoldRepository repository;
    private final LotRepository lotRepository;
    private final HoldReasonRepository holdReasonRepository;
    private final ReleaseRoleChecker releaseRoleChecker;
    private final LotHistoryApplicationService history;
    private final CurrentSessionProvider session;
    private final ConfirmPort confirms;
    private final Clock clock;

    /**
     * 手工 Hold。同一原因可以重复。
     *
     * @param lotId Lot
     * @param request 原因码和备注
     * @throws BusinessException 原因不存在、已停用，或 Lot 已终态
     */
    @Transactional
    public void hold(Long lotId, LotHoldRequest request) {
        Lot lot = lockOpenLot(lotId);
        HoldReason reason = requireEnabled(request.getReasonCode());
        insert(lot, reason, "MANUAL", null, request.getRemark(), 0L);
        history.record(lot, "HOLD", reason.getReasonCode(), request.getRemark(), null, lot.getStatus(), lot.getQty());
    }

    /**
     * 系统 Hold。同一幂等键已存在时不再插入，也不增加 hold_count。
     *
     * @param lotId Lot
     * @param source 来源，不能是 MANUAL
     * @param reasonCode 原因码
     * @param sourceRefId 来源单据，没有时传 null
     * @throws BusinessException 来源不合法、原因不可用，或 Lot 已终态
     */
    @Transactional
    public void holdSystem(Long lotId, String source, String reasonCode, Long sourceRefId) {
        if (source == null || "MANUAL".equals(source) || !SOURCES.contains(source)) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_SOURCE_INVALID, "Hold 来源不合法");
        }
        Lot lot = lockOpenLot(lotId);
        String key = "NCR".equals(source)
                ? lotId + "|NCR|" + sourceRefId
                : lotId + "|" + lot.getRouteSeqNo() + "|" + source + "|" + reasonCode;
        if (repository.findActiveByKey(key).isPresent()) {
            return;
        }
        HoldReason reason = requireEnabled(reasonCode);
        insert(lot, reason, source, key, null, sourceRefId == null ? 0L : sourceRefId);
        history.record(lot, "HOLD", reasonCode, null, null, lot.getStatus(), lot.getQty());
    }

    /**
     * 释放一条系统 Hold。没有活动记录时什么都不做。不检查确认和角色。
     *
     * @param lotId Lot
     * @param source 来源
     * @param reasonCode 原因码
     */
    @Transactional
    public void releaseSystem(Long lotId, String source, String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return;
        }
        Lot lot = lockOpenLot(lotId);
        String key = lotId + "|" + lot.getRouteSeqNo() + "|" + source + "|" + reasonCode;
        LotHold hold = repository.findActiveByKey(key).orElse(null);
        if (hold == null) {
            return;
        }
        hold.setStatus("RELEASED");
        hold.setActiveKey(null);
        hold.setReleaseBy(session.getCurrentUserNameOrDefault());
        hold.setReleaseAt(LocalDateTime.now(clock));
        repository.update(hold);
        int count = lot.getHoldCount() == null ? 0 : lot.getHoldCount();
        lot.setHoldCount(Math.max(0, count - 1));
        lotRepository.update(lot);
        history.record(lot, "RELEASE", reasonCode, null, null, lot.getStatus(), lot.getQty());
    }

    /**
     * 释放一条 Hold。
     *
     * @param lotId Lot
     * @param request Hold 记录和备注
     * @throws BusinessException 记录不存在、已释放，或操作人没有要求的角色
     */
    @Transactional
    public void release(Long lotId, LotReleaseRequest request) {
        if (!confirms.allowed("HOLD_RELEASE", "LOT", lotId)) {
            throw new BusinessException(MesExecErrorCodes.CONFIRM_REQUIRED, "解除 Hold 还没有完成确认");
        }
        Lot lot = lockOpenLot(lotId);
        LotHold hold = repository.findById(request.getHoldId())
                .filter(row -> lotId.equals(row.getLotId()))
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_HOLD_NOT_FOUND, "Hold 记录不存在"));
        if (!"ACTIVE".equals(hold.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_ALREADY_RELEASED, "Hold 已经释放");
        }
        String role = hold.getReleaseRoleCode();
        if (role != null && !role.isBlank() && !releaseRoleChecker.has(role)) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_ROLE_DENIED, "没有 Release 所需角色: " + role);
        }
        hold.setStatus("RELEASED");
        hold.setActiveKey(null);
        hold.setReleaseBy(session.getCurrentUserNameOrDefault());
        hold.setReleaseAt(LocalDateTime.now(clock));
        hold.setReleaseRemark(request.getRemark());
        repository.update(hold);
        int count = lot.getHoldCount() == null ? 0 : lot.getHoldCount();
        lot.setHoldCount(Math.max(0, count - 1));
        lotRepository.update(lot);
        history.record(lot, "RELEASE", hold.getReasonCode(), request.getRemark(), null, lot.getStatus(), lot.getQty());
    }

    /**
     * 列出这个 Lot 上尚未释放的 Hold。
     *
     * @param lotId Lot
     * @return 生效中的 Hold，没有时为空列表
     */
    public List<LotHold> listActive(Long lotId) {
        return repository.listActive(lotId);
    }

    private Lot lockOpenLot(Long lotId) {
        lotRepository.lock(lotId);
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_LOT_CLOSED, "终态 Lot 不能 Hold 或 Release");
        }
        return lot;
    }

    private HoldReason requireEnabled(String reasonCode) {
        HoldReason reason = holdReasonRepository.findByCode(reasonCode)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在"));
        if (reason.getEnabled() == null || reason.getEnabled() != 1) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_REASON_DISABLED, "Hold 原因已停用");
        }
        return reason;
    }

    private void insert(Lot lot, HoldReason reason, String source, String activeKey, String remark, long sourceRefId) {
        LotHold hold = new LotHold();
        hold.setLotId(lot.getId());
        hold.setReasonCode(reason.getReasonCode());
        hold.setHoldType(reason.getHoldType());
        hold.setHoldSource(source);
        hold.setStatus("ACTIVE");
        hold.setActiveKey(activeKey);
        hold.setHoldStepId(lot.getStepId());
        hold.setHoldSeqNo(lot.getRouteSeqNo());
        hold.setHoldBy(session.getCurrentUserNameOrDefault());
        hold.setHoldAt(LocalDateTime.now(clock));
        hold.setHoldRemark(remark);
        hold.setReleaseRoleCode(reason.getReleaseRoleCode());
        hold.setSourceRefId(sourceRefId);
        repository.save(hold);
        int count = lot.getHoldCount() == null ? 0 : lot.getHoldCount();
        lot.setHoldCount(count + 1);
        lotRepository.update(lot);
    }
}
