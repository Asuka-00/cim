package com.scaffold.system.api.v1.system;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.service.system.ReleaseApplicationService;
import com.scaffold.system.domain.vo.system.ReleaseActivateResultVO;
import com.scaffold.system.domain.vo.system.ReleaseCurrentVO;
import com.scaffold.system.domain.vo.system.ReleaseInfoVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 内网 zip 在线更版接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@RestController
@RequestMapping("/api/v1/system/releases")
@RequiredArgsConstructor
@Tag(name = "ReleaseController", description = "系统版本在线更版接口")
public class ReleaseControllerV1 {

    private final ReleaseApplicationService releaseApplicationService;

    @AuditLog(operation = "查询当前发布版本", logResponse = false, writeDb = false)
    @Operation(summary = "当前版本", description = "查询 current 软链与进程运行版本")
    @GetMapping("/current")
    public Result<ReleaseCurrentVO> current() {
        return Result.success(releaseApplicationService.getCurrent());
    }

    @AuditLog(operation = "查询已安装发布版本", logResponse = false, writeDb = false)
    @Operation(summary = "版本列表", description = "列出 releases 目录下已安装版本")
    @GetMapping
    public Result<List<ReleaseInfoVO>> list() {
        return Result.success(releaseApplicationService.listReleases());
    }

    @AuditLog(operation = "上传发布包", logResponse = false)
    @Operation(summary = "上传 zip 发布包", description = "zip 内含 app.jar 与 version.json")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ReleaseInfoVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(releaseApplicationService.upload(file));
    }

    @AuditLog(operation = "激活发布版本")
    @Operation(summary = "激活版本", description = "切换 current 软链并计划进程重启")
    @PostMapping("/{version}/activate")
    public Result<ReleaseActivateResultVO> activate(@PathVariable("version") String version) {
        return Result.success(releaseApplicationService.activate(version));
    }

    @AuditLog(operation = "回滚发布版本")
    @Operation(summary = "回滚", description = "切换到 previous 并计划进程重启")
    @PostMapping("/rollback")
    public Result<ReleaseActivateResultVO> rollback() {
        return Result.success(releaseApplicationService.rollback());
    }
}
