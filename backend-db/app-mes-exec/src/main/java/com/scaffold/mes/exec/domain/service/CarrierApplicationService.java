package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.CarrierCreateRequest;
import com.scaffold.mes.exec.domain.entity.Carrier;
import com.scaffold.mes.exec.domain.entity.CarrierBinding;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.CarrierBindingRepository;
import com.scaffold.mes.exec.domain.repository.CarrierRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * CarrierApplicationService 载具绑定
 * <p>
 * 一个载具同时只绑一个 Lot。载具上的 current_lot_id 是事实来源，Lot 上的编码只做展示。FRAME 不做整批绑定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class CarrierApplicationService {

    private static final Set<String> TYPES = Set.of("FOUP", "CASSETTE", "MAGAZINE", "OTHER");
    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final CarrierRepository carrierRepository;
    private final CarrierBindingRepository bindingRepository;
    private final LotRepository lotRepository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 列出全部载具。
     *
     * @return 按编码排列
     */
    public List<Carrier> list() {
        return carrierRepository.list();
    }

    /**
     * 新建空载具。
     *
     * @param request 编码和类型
     * @throws BusinessException 编码重复或类型不合法
     */
    @Transactional
    public void create(CarrierCreateRequest request) {
        if (carrierRepository.findByCode(request.getCarrierCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.CARRIER_CODE_EXISTS, "载具编码已存在");
        }
        String type = request.getCarrierType() == null || request.getCarrierType().isBlank() ? "FOUP" : request.getCarrierType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesExecErrorCodes.CARRIER_TYPE_INVALID, "载具类型不能做整批绑定");
        }
        Carrier carrier = new Carrier();
        carrier.setCarrierCode(request.getCarrierCode());
        carrier.setCarrierType(type);
        carrier.setCapacity(request.getCapacity() == null ? 25 : request.getCapacity());
        carrier.setStatus("EMPTY");
        carrier.setUseCount(0);
        carrier.setEnabled(1);
        carrier.setVersion(0);
        carrierRepository.save(carrier);
    }

    /**
     * 把空载具绑到在制 Lot。
     *
     * @param carrierId 载具
     * @param lotId Lot
     * @throws BusinessException 载具不空、Lot 已有载具，或 Lot 已终态
     */
    @Transactional
    public void bind(Long carrierId, Long lotId) {
        Carrier carrier = require(carrierId);
        if (carrier.getEnabled() == null || carrier.getEnabled() != 1 || carrier.getCurrentLotId() != null
                || !"EMPTY".equals(carrier.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.CARRIER_OCCUPIED, "载具现在不能绑定");
        }
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_ALREADY_ON_CARRIER, "终态 Lot 不能绑定载具");
        }
        if (carrierRepository.findByCurrentLot(lotId).isPresent() || (lot.getCarrierCode() != null && !lot.getCarrierCode().isBlank())) {
            throw new BusinessException(MesExecErrorCodes.LOT_ALREADY_ON_CARRIER, "Lot 已经绑在载具上");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        carrier.setCurrentLotId(lotId);
        carrier.setStatus("OCCUPIED");
        carrier.setUseCount((carrier.getUseCount() == null ? 0 : carrier.getUseCount()) + 1);
        carrierRepository.update(carrier);
        lot.setCarrierCode(carrier.getCarrierCode());
        lotRepository.update(lot);
        CarrierBinding binding = new CarrierBinding();
        binding.setCarrierId(carrierId);
        binding.setLotId(lotId);
        binding.setBoundAt(now);
        binding.setBoundBy(session.getCurrentUserNameOrDefault());
        bindingRepository.save(binding);
    }

    /**
     * 解绑。只回填绑定历史的结束时间。
     *
     * @param carrierId 载具
     * @throws BusinessException 载具不存在或本来就是空的
     */
    @Transactional
    public void unbind(Long carrierId) {
        Carrier carrier = require(carrierId);
        if (carrier.getCurrentLotId() == null) {
            throw new BusinessException(MesExecErrorCodes.CARRIER_OCCUPIED, "载具没有绑定 Lot");
        }
        Lot lot = lotRepository.findById(carrier.getCurrentLotId()).orElse(null);
        if (lot != null && carrier.getCarrierCode().equals(lot.getCarrierCode())) {
            lot.setCarrierCode(null);
            lotRepository.update(lot);
        }
        carrier.setCurrentLotId(null);
        carrier.setStatus("EMPTY");
        carrierRepository.update(carrier);
        bindingRepository.findOpen(carrierId).ifPresent(binding -> {
            binding.setUnboundAt(LocalDateTime.now(clock));
            binding.setUnboundBy(session.getCurrentUserNameOrDefault());
            bindingRepository.update(binding);
        });
    }

    private Carrier require(Long id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.CARRIER_NOT_FOUND, "载具不存在"));
    }
}
