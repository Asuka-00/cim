package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.UnitSerialRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.UnitSerial;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.UnitSerialRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ProductSerialPort;

import lombok.RequiredArgsConstructor;

/**
 * UnitSerialApplicationService 登记 Unit 序列号
 * <p>
 * 产品的 track_serial 不是 1 时拒绝。序列号在租户内不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UnitSerialApplicationService {

    private final LotRepository lotRepository;
    private final UnitSerialRepository serialRepository;
    private final ProductSerialPort productSerial;

    /**
     * 列出某个 Lot 的序列号。
     *
     * @param lotId Lot
     * @return 按序列号排列
     */
    public List<UnitSerial> list(Long lotId) {
        return serialRepository.listByLot(lotId);
    }

    /**
     * 给 Lot 登记一个序列号。
     *
     * @param lotId Lot
     * @param request 序列号和可选位置
     * @throws BusinessException 产品未打开跟踪，或序列号已存在
     */
    @Transactional
    public void assign(Long lotId, UnitSerialRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!productSerial.enabled(lot.getProductId())) {
            throw new BusinessException(MesExecErrorCodes.UNIT_SERIAL_DISABLED, "产品没有打开序列号跟踪");
        }
        if (serialRepository.findByNo(request.getSerialNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.UNIT_SERIAL_EXISTS, "序列号已存在");
        }
        UnitSerial serial = new UnitSerial();
        serial.setSerialNo(request.getSerialNo());
        serial.setLotId(lotId);
        serial.setStripId(request.getStripId());
        serial.setContainerId(request.getContainerId());
        serial.setPosition(request.getPosition());
        serial.setStatus("NORMAL");
        serialRepository.save(serial);
    }
}
