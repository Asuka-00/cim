package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.service.ApprovalRecordApplicationService;
import com.scaffold.share.context.UserContextHolder;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * ApprovalInboxControllerV1 待我审批
 * <p>
 * 只列出当前用户名下、尚未通过或驳回的提交。通过和驳回仍走各版本自己的接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/approvals")
@RequiredArgsConstructor
public class ApprovalInboxControllerV1 {

    private final ApprovalRecordApplicationService service;

    /**
     * 查询待我审批。
     *
     * @return 仍等当前用户处理的提交
     */
    @GetMapping
    public Result<List<ApprovalRecord>> mine() {
        return Result.success(service.listMine(UserContextHolder.getUserName()));
    }
}
