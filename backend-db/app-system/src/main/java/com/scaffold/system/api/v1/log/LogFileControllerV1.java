package com.scaffold.system.api.v1.log;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.log.LogFileInfoVO;
import com.scaffold.system.domain.log.LogFileService;
import com.scaffold.system.domain.log.LogTailVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * LogFileControllerV1 日志文件查看与下载接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/v1/audit/log-files")
@RequiredArgsConstructor
@Tag(name = "LogFileController", description = "日志文件查看与下载接口")
public class LogFileControllerV1 {

    private final LogFileService logFileService;

    @AuditLog(operation = "查询日志文件列表", logResponse = false, writeDb = false)
    @Operation(summary = "查询日志文件列表", description = "查询审计、错误或慢日志文件列表")
    @GetMapping
    public Result<List<LogFileInfoVO>> listFiles(@RequestParam("type") String type) {
        return Result.success(logFileService.listFiles(type));
    }

    @AuditLog(operation = "查看日志文件尾部", logResponse = false, writeDb = false)
    @Operation(summary = "查看日志文件尾部", description = "查看日志文件末尾N行")
    @GetMapping("/{type}/tail")
    public Result<LogTailVO> tail(
            @PathVariable("type") String type,
            @RequestParam("fileName") String fileName,
            @RequestParam(name = "lines", required = false) String lines) {
        return Result.success(logFileService.tail(type, fileName, lines));
    }

    @AuditLog(operation = "下载日志文件", logResponse = false, writeDb = false)
    @Operation(summary = "下载日志文件", description = "下载审计、错误或慢日志文件")
    @GetMapping("/{type}/download")
    public ResponseEntity<Resource> download(
            @PathVariable("type") String type,
            @RequestParam("fileName") String fileName) {
        Resource resource = logFileService.download(type, fileName);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(resource.getFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(logFileService.contentLength(resource))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
