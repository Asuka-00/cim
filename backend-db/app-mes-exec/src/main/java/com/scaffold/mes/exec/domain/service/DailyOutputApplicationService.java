package com.scaffold.mes.exec.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.dto.DailyOutputResponse;
import com.scaffold.mes.exec.domain.dto.ProductOutputResponse;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;

import lombok.RequiredArgsConstructor;

/**
 * DailyOutputApplicationService 生产日报
 * <p>
 * 统计某一天完成的 Lot 数、数量和报废数量。完成时间按本地日期切，不含合批结束的批。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class DailyOutputApplicationService {

    private final LotRepository lots;

    /**
     * 汇总这一天的完工量。
     *
     * @param day 日期
     * @return 批数和数量。没有完工时都是 0
     */
    public DailyOutputResponse summarize(LocalDate day) {
        long qty = 0;
        long scrap = 0;
        var rows = lots.listCompletedBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        for (Lot lot : rows) {
            qty += lot.getQty() == null ? 0 : lot.getQty();
            scrap += lot.getScrapQty() == null ? 0 : lot.getScrapQty();
        }
        return new DailyOutputResponse(rows.size(), qty, scrap);
    }

    /**
     * 按产品汇总这一天的完工量。
     *
     * @param day 日期
     * @return 每个产品一行。没有完工时为空列表
     */
    public List<ProductOutputResponse> byProduct(LocalDate day) {
        Map<Long, long[]> buckets = new LinkedHashMap<>();
        for (Lot lot : lots.listCompletedBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            long[] bucket = buckets.computeIfAbsent(lot.getProductId(), id -> new long[3]);
            bucket[0]++;
            bucket[1] += lot.getQty() == null ? 0 : lot.getQty();
            bucket[2] += lot.getScrapQty() == null ? 0 : lot.getScrapQty();
        }
        List<ProductOutputResponse> lines = new ArrayList<>();
        buckets.forEach((productId, bucket) -> lines.add(new ProductOutputResponse(productId, bucket[0], bucket[1], bucket[2])));
        return lines;
    }
}
