package com.scaffold.mes.exec.api.v1;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.CollectResultRequest;
import com.scaffold.mes.exec.domain.dto.CollectVoidRequest;
import com.scaffold.mes.exec.domain.service.CollectResultApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CollectResultControllerV1 采集录入
 * <p>
 * 保存加工中 Lot 的原始采集值。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/collect-results")
@RequiredArgsConstructor
public class CollectResultControllerV1 {

    private final CollectResultApplicationService service;

    /**
     * 录入一条原始数值。
     *
     * @param request Lot、采集项和数值
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "录入采集值")
    public Result<Boolean> record(@Valid @RequestBody CollectResultRequest request) {
        service.record(request.getLotId(), request.getItemId(), request.getValueNum());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更正当前经过的有效值。
     *
     * @param request Lot、采集项和更正后的数值
     * @return 固定为 true
     */
    @PostMapping("/correct")
    @AuditLog(operation = "更正采集值")
    public Result<Boolean> correct(@Valid @RequestBody CollectResultRequest request) {
        service.correct(request.getLotId(), request.getItemId(), request.getValueNum());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 作废当前经过的最后一条采集值。
     *
     * @param request Lot 和采集项
     * @return 固定为 true
     */
    @PostMapping("/void")
    @AuditLog(operation = "撤销采集")
    public Result<Boolean> voidLatest(@Valid @RequestBody CollectVoidRequest request) {
        service.voidLatest(request.getLotId(), request.getItemId());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询这个采集点的有效值。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @param routeSeqNo 步骤序号
     * @param passNo 经过次数
     * @return 最后一条的数值。没有或已作废时为空
     */
    @GetMapping("/effective")
    public Result<BigDecimal> effective(@RequestParam Long lotId, @RequestParam Long itemId,
            @RequestParam Integer routeSeqNo, @RequestParam Integer passNo) {
        return Result.success(service.effective(lotId, itemId, routeSeqNo, passNo).orElse(null));
    }
}
