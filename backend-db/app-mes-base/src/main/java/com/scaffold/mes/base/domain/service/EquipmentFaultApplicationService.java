package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentFaultCloseRequest;
import com.scaffold.mes.base.domain.dto.EquipmentFaultOpenRequest;
import com.scaffold.mes.base.domain.dto.EquipmentStatusChangeRequest;
import com.scaffold.mes.base.domain.entity.EquipmentFault;
import com.scaffold.mes.base.domain.entity.EquipmentFaultLot;
import com.scaffold.mes.base.domain.repository.EquipmentFaultRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentFaultApplicationService 设备异常
 * <p>
 * 登记描述和受影响的 Lot。关闭时写处理结果。只有勾选同时置停机时才改设备状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentFaultApplicationService {

    private static final Set<String> SEVERITY = Set.of("MINOR", "MAJOR", "CRITICAL");

    private final EquipmentFaultRepository repository;
    private final EquipmentStatusApplicationService equipmentStatus;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 列出全部异常。
     *
     * @return 按单号排列
     */
    public List<EquipmentFault> list() {
        return repository.list();
    }

    /**
     * 登记一条未关闭的异常。
     *
     * @param request 单号、设备和描述
     * @throws BusinessException 单号重复或描述为空
     */
    @Transactional
    public void open(EquipmentFaultOpenRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_INVALID, "异常描述不能为空");
        }
        if (repository.findByNo(request.getFaultNo()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_INVALID, "异常单号已存在");
        }
        String severity = request.getSeverity() == null || request.getSeverity().isBlank() ? "MINOR" : request.getSeverity();
        if (!SEVERITY.contains(severity)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_INVALID, "异常等级不合法");
        }
        EquipmentFault fault = new EquipmentFault();
        fault.setId(AppIdGenerator.nextId());
        fault.setFaultNo(request.getFaultNo());
        fault.setEquipmentId(request.getEquipmentId());
        fault.setSeverity(severity);
        fault.setDescription(request.getDescription());
        fault.setOccurredAt(LocalDateTime.now(clock));
        fault.setReportedBy(session.getCurrentUserNameOrDefault());
        fault.setStatus("OPEN");
        int lots = request.getLotIds() == null ? 0 : request.getLotIds().size();
        fault.setAffectedLotCount(lots);
        repository.save(fault);
        if (request.getLotIds() != null) {
            for (Long lotId : request.getLotIds()) {
                EquipmentFaultLot link = new EquipmentFaultLot();
                link.setFaultId(fault.getId());
                link.setLotId(lotId);
                repository.saveLot(link);
            }
        }
        if (Boolean.TRUE.equals(request.getDownEquipment())) {
            EquipmentStatusChangeRequest down = new EquipmentStatusChangeRequest();
            down.setE10State("UNSCHEDULED_DOWN");
            down.setReasonCode("FAULT");
            down.setRemark(request.getFaultNo());
            equipmentStatus.change(request.getEquipmentId(), down);
        }
    }

    /**
     * 关闭异常。
     *
     * @param faultId 异常
     * @param request 处理结果
     * @throws BusinessException 已关闭或结果为空
     */
    @Transactional
    public void close(Long faultId, EquipmentFaultCloseRequest request) {
        if (request.getSolution() == null || request.getSolution().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_INVALID, "处理结果不能为空");
        }
        EquipmentFault fault = repository.findById(faultId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_CLOSED, "异常单不存在"));
        if ("CLOSED".equals(fault.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_FAULT_CLOSED, "异常单已关闭");
        }
        fault.setStatus("CLOSED");
        fault.setSolution(request.getSolution());
        repository.update(fault);
    }
}
