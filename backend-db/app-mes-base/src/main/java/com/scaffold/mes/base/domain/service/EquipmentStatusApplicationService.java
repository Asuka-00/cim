package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentStatusChangeRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.EquipmentStatusHistRepository;
import com.scaffold.share.context.UserContextHolder;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentStatusApplicationService 设备状态应用服务
 * <p>
 * 切换时先结束进行中的时间段并回填秒数，再打开新段。设备级用 portId=0。腔体用自己的主键，并更新腔体当前状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentStatusApplicationService {

    /** 设备级状态。腔体用 mes_equipment_port 的主键。 */
    static final long EQUIPMENT_PORT = 0L;

    private static final Set<String> E10 = Set.of(
            "PRODUCTIVE", "STANDBY", "ENGINEERING", "SCHEDULED_DOWN", "UNSCHEDULED_DOWN", "NON_SCHEDULED");
    private static final Set<String> DOWN = Set.of("SCHEDULED_DOWN", "UNSCHEDULED_DOWN");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentPortRepository portRepository;
    private final EquipmentStatusHistRepository histRepository;
    private final Clock clock;

    /**
     * 分页查询设备状态历史。
     *
     * @param equipmentId 设备
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 按开始时间倒序的分页结果
     * @throws BusinessException 设备不存在
     */
    public PageDTO<EquipmentStatusHist> history(Long equipmentId, int page, int pageSize) {
        requireEquipment(equipmentId);
        return histRepository.page(equipmentId, page, pageSize);
    }

    /**
     * 删掉还没切换过的初始时间段，便于删除从未改过状态的设备。
     *
     * @param equipmentId 设备
     */
    @Transactional
    public void discardUntouchedInitial(Long equipmentId) {
        histRepository.deleteUntouchedInitial(equipmentId);
    }

    /**
     * 为新建台账打开第一段。已有进行中的段时不再插入。
     *
     * @param equipment 刚保存的设备，主键已生成
     */
    @Transactional
    public void openInitial(Equipment equipment) {
        if (!histRepository.listOpen(equipment.getId(), EQUIPMENT_PORT).isEmpty()) {
            return;
        }
        histRepository.save(segment(equipment.getId(), EQUIPMENT_PORT, null, equipment.getE10State(), equipment.getStatusCode(),
                equipment.getReasonCode(), equipment.getStatusSince(), equipment.getStatusSource(), null));
    }

    /**
     * 为新建腔体打开第一段。普通 Port 沿用设备状态，不单独记历史。
     *
     * @param port 刚保存的端口，主键已生成
     */
    @Transactional
    public void openInitialChamber(EquipmentPort port) {
        if (!"CHAMBER".equals(port.getKind()) || port.getId() == null) {
            return;
        }
        if (!histRepository.listOpen(port.getEquipmentId(), port.getId()).isEmpty()) {
            return;
        }
        histRepository.save(segment(port.getEquipmentId(), port.getId(), null, port.getE10State(), port.getStatusCode(),
                port.getReasonCode(), port.getStatusSince(), port.getStatusSource(), null));
    }

    /**
     * 手工切换设备级状态。
     *
     * @param equipmentId 设备
     * @param request 目标状态
     * @throws BusinessException 设备不存在、E10 非法、停机缺少原因码，或时间段倒挂
     */
    @Transactional
    public void change(Long equipmentId, EquipmentStatusChangeRequest request) {
        Equipment equipment = requireEquipment(equipmentId);
        String target = request.getE10State();
        if (!E10.contains(target)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_E10_INVALID, "E10 状态不合法: " + target);
        }
        if (DOWN.contains(target) && (request.getReasonCode() == null || request.getReasonCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_REASON_REQUIRED, "停机状态必须填写原因码");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        closeOpen(equipment.getId(), EQUIPMENT_PORT, equipment.getStatusSince(), equipment.getE10State(),
                equipment.getStatusCode(), equipment.getReasonCode(), equipment.getStatusSource(), now);
        histRepository.save(segment(equipmentId, EQUIPMENT_PORT, equipment.getE10State(), target, request.getStatusCode(),
                request.getReasonCode(), now, "MANUAL", request.getRemark()));
        equipment.setE10State(target);
        equipment.setStatusCode(request.getStatusCode());
        equipment.setReasonCode(request.getReasonCode());
        equipment.setStatusSource("MANUAL");
        equipment.setStatusSince(now);
        equipmentRepository.update(equipment);
    }

    /**
     * 进站联动。来源记为 TRACK，不要求停机原因码。
     *
     * @param equipmentId 设备
     * @param e10State PRODUCTIVE 或 ENGINEERING
     * @throws BusinessException 设备不存在或状态不合法
     */
    @Transactional
    public void applyTrack(Long equipmentId, String e10State) {
        Equipment equipment = requireEquipment(equipmentId);
        if (!E10.contains(e10State)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_E10_INVALID, "E10 状态不合法: " + e10State);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        closeOpen(equipment.getId(), EQUIPMENT_PORT, equipment.getStatusSince(), equipment.getE10State(),
                equipment.getStatusCode(), equipment.getReasonCode(), equipment.getStatusSource(), now);
        histRepository.save(segment(equipmentId, EQUIPMENT_PORT, equipment.getE10State(), e10State, null,
                null, now, "TRACK", null));
        equipment.setE10State(e10State);
        equipment.setStatusCode(null);
        equipment.setReasonCode(null);
        equipment.setStatusSource("TRACK");
        equipment.setStatusSince(now);
        equipmentRepository.update(equipment);
    }

    /**
     * 手工切换腔体状态。历史行的 portId 为腔体主键。
     *
     * @param equipmentId 设备
     * @param portId 腔体
     * @param request 目标状态
     * @throws BusinessException 不是腔体、E10 非法、停机缺少原因码，或时间段倒挂
     */
    @Transactional
    public void changeChamber(Long equipmentId, Long portId, EquipmentStatusChangeRequest request) {
        EquipmentPort port = requireChamber(equipmentId, portId);
        String target = request.getE10State();
        if (!E10.contains(target)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_E10_INVALID, "E10 状态不合法: " + target);
        }
        if (DOWN.contains(target) && (request.getReasonCode() == null || request.getReasonCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_REASON_REQUIRED, "停机状态必须填写原因码");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        closeOpen(equipmentId, portId, port.getStatusSince(), port.getE10State(), port.getStatusCode(),
                port.getReasonCode(), port.getStatusSource(), now);
        histRepository.save(segment(equipmentId, portId, port.getE10State(), target, request.getStatusCode(),
                request.getReasonCode(), now, "MANUAL", request.getRemark()));
        port.setE10State(target);
        port.setStatusCode(request.getStatusCode());
        port.setReasonCode(request.getReasonCode());
        port.setStatusSource("MANUAL");
        port.setStatusSince(now);
        portRepository.update(port);
    }

    private void closeOpen(Long equipmentId, Long portId, LocalDateTime statusSince, String e10State, String statusCode,
            String reasonCode, String source, LocalDateTime now) {
        List<EquipmentStatusHist> open = histRepository.listOpen(equipmentId, portId);
        if (open.isEmpty() && statusSince != null && !statusSince.isAfter(now)) {
            EquipmentStatusHist gap = segment(equipmentId, portId, null, e10State, statusCode, reasonCode, statusSince, source, null);
            finish(gap, now);
            histRepository.save(gap);
            return;
        }
        for (EquipmentStatusHist hist : open) {
            finish(hist, now);
            histRepository.update(hist);
        }
    }

    private static void finish(EquipmentStatusHist hist, LocalDateTime end) {
        if (hist.getStartAt() != null && hist.getStartAt().isAfter(end)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_STATUS_TIME_INVALID, "状态结束时间早于开始时间");
        }
        hist.setEndAt(end);
        hist.setDurationSec(hist.getStartAt() == null ? 0L : ChronoUnit.SECONDS.between(hist.getStartAt(), end));
    }

    private EquipmentPort requireChamber(Long equipmentId, Long portId) {
        EquipmentPort port = portRepository.findById(portId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_NOT_FOUND, "设备端口不存在"));
        if (!equipmentId.equals(port.getEquipmentId())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_NOT_FOUND, "设备端口不存在");
        }
        if (!"CHAMBER".equals(port.getKind())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_KIND_INVALID, "只有腔体可以单独切换状态");
        }
        return port;
    }

    private Equipment requireEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在"));
    }

    private static EquipmentStatusHist segment(Long equipmentId, Long portId, String from, String to, String statusCode,
            String reasonCode, LocalDateTime start, String source, String remark) {
        EquipmentStatusHist hist = new EquipmentStatusHist();
        hist.setEquipmentId(equipmentId);
        hist.setPortId(portId);
        hist.setFromE10State(from);
        hist.setToE10State(to);
        hist.setStatusCode(statusCode);
        hist.setReasonCode(reasonCode);
        hist.setStartAt(start);
        hist.setSource(source == null ? "MANUAL" : source);
        hist.setOperator(UserContextHolder.getUserName());
        hist.setRemark(remark);
        return hist;
    }
}
