package com.scaffold.mes.exec.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotConsumptionResponse;
import com.scaffold.mes.exec.domain.entity.MaterialConsumption;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.MaterialConsumptionRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotConsumptionQueryApplicationService 物料消耗追溯
 * <p>
 * 按发生时间列出这个 Lot 的消耗流水，包含欠账和回冲。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotConsumptionQueryApplicationService {

    private final LotRepository lots;
    private final MaterialConsumptionRepository consumptions;

    /**
     * 列出消耗。
     *
     * @param lotId Lot
     * @return 消耗，没有时为空列表
     * @throws BusinessException Lot 不存在
     */
    public List<LotConsumptionResponse> list(Long lotId) {
        if (lots.findById(lotId).isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在");
        }
        List<LotConsumptionResponse> rows = new ArrayList<>();
        for (MaterialConsumption row : consumptions.listByLot(lotId)) {
            rows.add(new LotConsumptionResponse(row.getId(), row.getMaterialId(), row.getMaterialLotId(), row.getQty(),
                    row.getSource(), Integer.valueOf(1).equals(row.getIsShortage())));
        }
        return rows;
    }
}
