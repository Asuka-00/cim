package com.scaffold.mes.exec.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.StripConvertRequest;
import com.scaffold.mes.exec.domain.dto.StripCreateRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Strip;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.StripRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StripApplicationService 建立 Strip
 * <p>
 * 只挂到等待或加工中的 Lot。良品数不能超过含 Unit 数。Lot 的副数量加上这次的 Unit 数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StripApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final LotRepository lotRepository;
    private final StripRepository stripRepository;

    /**
     * 列出某个 Lot 的 Strip。
     *
     * @param lotId Lot
     * @return Strip 列表
     */
    public List<Strip> list(Long lotId) {
        return stripRepository.listByLot(lotId);
    }

    /**
     * 建立一条 Strip。
     *
     * @param lotId Lot
     * @param request 编号和数量
     * @throws BusinessException 编号重复、Lot 已终态，或数量不合法
     */
    @Transactional
    public void create(Long lotId, StripCreateRequest request) {
        if (request.getUnitQty() == null || request.getUnitQty() < 1) {
            throw new BusinessException(MesExecErrorCodes.STRIP_QTY, "Strip 的 Unit 数至少为 1");
        }
        int good = request.getGoodQty() == null ? request.getUnitQty() : request.getGoodQty();
        if (good < 0 || good > request.getUnitQty()) {
            throw new BusinessException(MesExecErrorCodes.STRIP_QTY, "良品数不能超过 Unit 数");
        }
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.STRIP_LOT_CLOSED, "只有在制 Lot 可以建立 Strip");
        }
        if (stripRepository.findByCode(request.getStripCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.STRIP_CODE_EXISTS, "Strip 编号已存在");
        }
        Strip strip = new Strip();
        strip.setStripCode(request.getStripCode());
        strip.setLotId(lotId);
        strip.setStatus("NORMAL");
        strip.setUnitQty(request.getUnitQty());
        strip.setGoodQty(good);
        strip.setVersion(0);
        stripRepository.save(strip);
        long sub = lot.getSubQty() == null ? 0L : lot.getSubQty();
        lot.setSubQty(sub + request.getUnitQty());
        lotRepository.update(lot);
    }

    /**
     * 切筋成型。NORMAL 的 Strip 变为 CONVERTED，Lot 主数量改为良品合计。
     *
     * @param lotId Lot
     * @param request Unit 计数单位
     * @throws BusinessException Lot 已终态，或没有 NORMAL 的 Strip
     */
    @Transactional
    public void convert(Long lotId, StripConvertRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.STRIP_LOT_CLOSED, "只有在制 Lot 可以切筋成型");
        }
        List<Strip> normal = stripRepository.listByLot(lotId).stream()
                .filter(row -> "NORMAL".equals(row.getStatus()))
                .toList();
        if (normal.isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.STRIP_CONVERT_EMPTY, "没有可切筋成型的 Strip");
        }
        long units = 0L;
        for (Strip strip : normal) {
            strip.setStatus("CONVERTED");
            stripRepository.update(strip);
            units += strip.getGoodQty() == null ? 0 : strip.getGoodQty();
        }
        lot.setQty(units);
        lot.setQtyUnitId(request.getUnitId());
        lotRepository.update(lot);
    }
}
