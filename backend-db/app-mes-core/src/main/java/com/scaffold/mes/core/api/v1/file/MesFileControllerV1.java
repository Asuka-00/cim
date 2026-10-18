package com.scaffold.mes.core.api.v1.file;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.file.MesFileRegisterRequest;
import com.scaffold.mes.core.domain.dto.file.MesFileResponse;
import com.scaffold.mes.core.domain.service.file.MesFileApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * MesFileControllerV1 文件登记
 * <p>
 * 登记挂在 Lot 上的文件元数据。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/files")
@RequiredArgsConstructor
public class MesFileControllerV1 {

    private final MesFileApplicationService service;

    /**
     * 登记文件。
     *
     * @param request 文件名、大小、摘要和 Lot
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "登记 Lot 文件")
    public Result<Boolean> register(@Valid @RequestBody MesFileRegisterRequest request) {
        service.register(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出挂在这个 Lot 上的文件。
     *
     * @param lotId Lot
     * @return 文件
     */
    @GetMapping
    public Result<List<MesFileResponse>> listByLot(@RequestParam Long lotId) {
        return Result.success(service.listByLot(lotId));
    }

    /**
     * 写入文件内容。
     *
     * @param id 文件
     * @param body 内容
     * @return 固定为 true
     */
    @PutMapping(value = "/{id}/content", consumes = "application/octet-stream")
    @AuditLog(operation = "写入文件内容")
    public Result<Boolean> store(@PathVariable Long id, @RequestBody byte[] body) {
        service.store(id, body);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 读出文件内容。
     *
     * @param id 文件
     * @return 内容
     */
    @GetMapping(value = "/{id}/content", produces = "application/octet-stream")
    public byte[] load(@PathVariable Long id) {
        return service.load(id);
    }
}
