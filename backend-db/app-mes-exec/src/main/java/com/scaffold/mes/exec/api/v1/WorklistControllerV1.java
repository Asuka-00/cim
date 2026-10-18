package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WorklistItem;
import com.scaffold.mes.exec.domain.service.WorklistApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * WorklistControllerV1 作业清单
 * <p>
 * 按站点、区域或设备查看待加工 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/worklist")
@RequiredArgsConstructor
public class WorklistControllerV1 {

    private final WorklistApplicationService service;

    /**
     * 查询待加工队列。
     *
     * @param stepId 站点，可空
     * @param areaId 区域，可空
     * @param equipmentId 设备，可空
     * @return 排序后的队列
     */
    @GetMapping
    public Result<List<WorklistItem>> list(@RequestParam(required = false) Long stepId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long ruleId) {
        return Result.success(service.list(stepId, areaId, equipmentId, ruleId));
    }
}
