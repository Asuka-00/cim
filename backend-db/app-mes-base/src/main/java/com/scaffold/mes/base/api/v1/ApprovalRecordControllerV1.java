package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.service.ApprovalRecordApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * ApprovalRecordControllerV1 审批记录查询接口
 * <p>
 * 只提供查询。写入由版本提交和审批服务调用，避免绕过状态机直接补记录。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/approval-records")
@RequiredArgsConstructor
public class ApprovalRecordControllerV1 {

    private final ApprovalRecordApplicationService service;

    /**
     * 按业务对象分页查询审批记录。
     *
     * @param bizType 业务类型
     * @param bizId 业务对象主键
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<ApprovalRecord>> page(@RequestParam String bizType, @RequestParam Long bizId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.page(bizType, bizId, page, pageSize));
    }
}
