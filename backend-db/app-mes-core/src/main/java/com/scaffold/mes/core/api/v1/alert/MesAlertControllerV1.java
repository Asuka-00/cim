package com.scaffold.mes.core.api.v1.alert;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.mes.core.domain.service.alert.MesAlertApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MesAlertControllerV1 站内预警接口
 * <p>
 * 查询预警,并标记已读或关闭。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/alerts")
@RequiredArgsConstructor
public class MesAlertControllerV1 {

    private final MesAlertApplicationService service;

    /**
     * 分页查询预警。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param status 状态,可空
     * @return 预警分页
     */
    @GetMapping
    public Result<PageDTO<MesAlert>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(service.page(page, pageSize, status));
    }

    /**
     * 把预警标记为已读。
     *
     * @param id 预警主键
     * @return 固定为 true
     * @throws BusinessException 预警不存在
     */
    @PostMapping("/{id}/read")
    @AuditLog(operation = "预警已读")
    public Result<Boolean> read(@PathVariable Long id) {
        service.read(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 关闭预警。
     *
     * @param id 预警主键
     * @return 固定为 true
     * @throws BusinessException 预警不存在
     */
    @PostMapping("/{id}/close")
    @AuditLog(operation = "关闭预警")
    public Result<Boolean> close(@PathVariable Long id) {
        service.close(id);
        return Result.success(Boolean.TRUE);
    }
}
