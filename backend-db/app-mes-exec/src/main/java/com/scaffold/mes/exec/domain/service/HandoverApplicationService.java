package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.HandoverConfirmRequest;
import com.scaffold.mes.exec.domain.dto.HandoverCreateRequest;
import com.scaffold.mes.exec.domain.entity.Handover;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.HandoverRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentAbnormalPort;

import lombok.RequiredArgsConstructor;

/**
 * HandoverApplicationService 交接班
 * <p>
 * 新建时状态固定为 DRAFT。单号不能重复。提交时写入在制、Hold 批号、非计划停机台数和未关闭预警条数。只有草稿可以提交，只有已提交的可以确认。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class HandoverApplicationService {

    private final HandoverRepository repository;

    private final LotRepository lots;

    private final EquipmentAbnormalPort abnormalEquipment;

    private final MesAlertRepository alerts;

    private final Clock clock;

    /**
     * 列出全部交接班。
     *
     * @return 按单号排列
     */
    public List<Handover> list() {
        return repository.list();
    }

    /**
     * 新建草稿。
     *
     * @param request 单号、交班人和备注
     * @throws BusinessException 单号已存在
     */
    @Transactional
    public void create(HandoverCreateRequest request) {
        if (repository.findByNo(request.getHandoverNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.HANDOVER_INVALID, "交接单号已存在");
        }
        Handover handover = new Handover();
        handover.setHandoverNo(request.getHandoverNo());
        handover.setFromUser(request.getFromUser());
        handover.setRemark(request.getRemark());
        handover.setStatus("DRAFT");
        repository.save(handover);
    }

    /**
     * 提交草稿。
     *
     * @param id 交接班
     * @throws BusinessException 记录不存在，或不是草稿
     */
    @Transactional
    public void submit(Long id) {
        Handover handover = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.HANDOVER_STATUS, "交接班不存在"));
        if (!"DRAFT".equals(handover.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.HANDOVER_STATUS, "只有草稿可以提交");
        }
        long lotCount = 0;
        long qty = 0;
        long holdCount = 0;
        StringBuilder held = new StringBuilder();
        for (Lot lot : lots.listInProcess()) {
            lotCount++;
            qty += lot.getQty() == null ? 0 : lot.getQty();
            if (lot.getHoldCount() != null && lot.getHoldCount() > 0) {
                holdCount++;
                if (held.length() > 0) {
                    held.append(',');
                }
                held.append('"').append(jsonText(lot.getLotNo())).append('"');
            }
        }
        handover.setStatus("SUBMITTED");
        handover.setSubmittedAt(LocalDateTime.now(clock));
        handover.setContentJson("{\"wipLotCount\":" + lotCount + ",\"wipQty\":" + qty
                + ",\"wipHoldCount\":" + holdCount + ",\"holdLotNos\":[" + held
                + "],\"abnormalEquipmentCount\":" + abnormalEquipment.count()
                + ",\"openAlertCount\":" + alerts.countOpen() + "}");
        repository.update(handover);
    }

    /**
     * 接班人确认已提交的交接班。
     *
     * @param id 交接班
     * @param request 接班人
     * @throws BusinessException 记录不存在，不是已提交，或没写接班人
     */
    @Transactional
    public void confirm(Long id, HandoverConfirmRequest request) {
        if (request.getToUser() == null || request.getToUser().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.HANDOVER_STATUS, "确认必须填写接班人");
        }
        Handover handover = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.HANDOVER_STATUS, "交接班不存在"));
        if (!"SUBMITTED".equals(handover.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.HANDOVER_STATUS, "只有已提交的交接班可以确认");
        }
        handover.setStatus("CONFIRMED");
        handover.setToUser(request.getToUser());
        handover.setConfirmedAt(LocalDateTime.now(clock));
        repository.update(handover);
    }

    private static String jsonText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
