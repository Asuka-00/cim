package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.EsignRequest;
import com.scaffold.mes.base.domain.service.EsignApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EsignControllerV1 电子签名
 * <p>
 * 需要确认的动作写下签名。不需要确认时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/esigns")
@RequiredArgsConstructor
public class EsignControllerV1 {

    private final EsignApplicationService service;

    /**
     * 签名。
     *
     * @param request 动作、对象和签名人
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "电子签名")
    public Result<Boolean> sign(@Valid @RequestBody EsignRequest request) {
        service.sign(request);
        return Result.success(Boolean.TRUE);
    }
}
