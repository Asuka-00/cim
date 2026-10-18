package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.PackContainerCreateRequest;
import com.scaffold.mes.exec.domain.dto.PackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.PackContainer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.PackContainerRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * PackContainerApplicationService 包装容器
 * <p>
 * 新建时空容器。装入后数量达到容量即变为 FULL。不能装超过容量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class PackContainerApplicationService {

    private static final Set<String> TYPES = Set.of("TRAY", "REEL", "TUBE", "BOX");
    private static final Set<String> OPEN_LOT = Set.of("WAITING", "RUNNING");

    private final LotRepository lotRepository;
    private final PackContainerRepository containerRepository;
    private final Clock clock;

    /**
     * 列出某个 Lot 的容器。
     *
     * @param lotId Lot
     * @return 按容器号排列
     */
    public List<PackContainer> list(Long lotId) {
        return containerRepository.listByLot(lotId);
    }

    /**
     * 给 Lot 建一个空容器。
     *
     * @param lotId Lot
     * @param request 容器号、类型和容量
     * @throws BusinessException 类型或容量不合法，或容器号已存在
     */
    @Transactional
    public void create(Long lotId, PackContainerCreateRequest request) {
        if (request.getContainerType() == null || !TYPES.contains(request.getContainerType())
                || request.getCapacity() == null || request.getCapacity() < 1) {
            throw new BusinessException(MesExecErrorCodes.PACK_TYPE, "容器类型或容量不合法");
        }
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN_LOT.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.STRIP_LOT_CLOSED, "只有在制 Lot 可以建容器");
        }
        if (containerRepository.findByNo(request.getContainerNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.PACK_NO_EXISTS, "容器号已存在");
        }
        PackContainer container = new PackContainer();
        container.setContainerNo(request.getContainerNo());
        container.setContainerType(request.getContainerType());
        container.setLotId(lotId);
        container.setQty(0);
        container.setCapacity(request.getCapacity());
        container.setStatus("OPEN");
        container.setVersion(0);
        containerRepository.save(container);
    }

    /**
     * 往容器装入数量。达到容量时变为 FULL。
     *
     * @param containerId 容器
     * @param request 本次数量
     * @throws BusinessException 容器不是 OPEN，或装入后超过容量
     */
    @Transactional
    public void pack(Long containerId, PackRequest request) {
        if (request.getQty() == null || request.getQty() < 1) {
            throw new BusinessException(MesExecErrorCodes.PACK_QTY, "装入数量至少为 1");
        }
        PackContainer container = containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "容器不存在"));
        if (!"OPEN".equals(container.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.PACK_QTY, "只有 OPEN 的容器可以装入");
        }
        int next = (container.getQty() == null ? 0 : container.getQty()) + request.getQty();
        if (container.getCapacity() == null || next > container.getCapacity()) {
            throw new BusinessException(MesExecErrorCodes.PACK_QTY, "装入数量超过容量");
        }
        container.setQty(next);
        if (next == container.getCapacity()) {
            container.setStatus("FULL");
            container.setPackedAt(LocalDateTime.now(clock));
        }
        containerRepository.update(container);
    }
}
