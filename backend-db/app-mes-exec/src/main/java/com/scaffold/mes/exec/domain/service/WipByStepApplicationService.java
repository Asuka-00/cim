package com.scaffold.mes.exec.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.dto.WipStepResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;

import lombok.RequiredArgsConstructor;

/**
 * WipByStepApplicationService 在制分布
 * <p>
 * 按站点统计等待和加工中的 Lot，并分开其中被 Hold 的批。完成、合批结束和终止的批不计入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WipByStepApplicationService {

    private final LotRepository lots;

    /**
     * 按站点汇总在制量。
     *
     * @return 每个站点一行。没有在制批时为空列表
     */
    public List<WipStepResponse> summarize() {
        Map<Long, long[]> buckets = new LinkedHashMap<>();
        for (Lot lot : lots.listInProcess()) {
            long[] bucket = buckets.computeIfAbsent(lot.getStepId(), id -> new long[3]);
            bucket[0]++;
            bucket[1] += lot.getQty() == null ? 0 : lot.getQty();
            if (lot.getHoldCount() != null && lot.getHoldCount() > 0) {
                bucket[2]++;
            }
        }
        List<WipStepResponse> lines = new ArrayList<>();
        buckets.forEach((stepId, bucket) -> lines.add(new WipStepResponse(stepId, bucket[0], bucket[1], bucket[2])));
        return lines;
    }
}
