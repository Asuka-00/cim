package com.scaffold.mes.core.domain.service.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.file.MesFileRegisterRequest;
import com.scaffold.mes.core.domain.dto.file.MesFileResponse;
import com.scaffold.mes.core.domain.entity.file.MesFile;
import com.scaffold.mes.core.domain.repository.file.MesFileRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * MesFileApplicationService 文件登记
 * <p>
 * 登记挂在 Lot 上的元数据。内容写到 app.mes.file.root 下的相对路径，摘要或大小不符时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
public class MesFileApplicationService {

    private final MesFileRepository files;
    private final Path root;

    /**
     * 创建文件服务。
     *
     * @param files 文件仓储
     * @param root 本地根目录，配置项 app.mes.file.root
     */
    public MesFileApplicationService(MesFileRepository files, @Value("${app.mes.file.root:mes-files}") String root) {
        this.files = files;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    /**
     * 登记一个文件。
     *
     * @param request 文件名、大小、摘要和 Lot
     * @throws BusinessException 大小为负，或摘要不是 64 位十六进制
     */
    @Transactional
    public void register(MesFileRegisterRequest request) {
        if (request.getSizeBytes() == null || request.getSizeBytes() < 0 || !sha256(request.getSha256())) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件大小或摘要不合法");
        }
        MesFile file = new MesFile();
        file.setFileName(request.getFileName());
        file.setContentType(request.getContentType());
        file.setSizeBytes(request.getSizeBytes());
        file.setSha256(request.getSha256().toLowerCase());
        file.setBizType("LOT");
        file.setBizId(request.getLotId());
        file.setStoragePath("lot/" + request.getLotId() + "/" + file.getSha256());
        files.save(file);
    }

    /**
     * 列出挂在这个 Lot 上的文件。
     *
     * @param lotId Lot
     * @return 文件，没有时为空列表
     */
    public List<MesFileResponse> listByLot(Long lotId) {
        List<MesFileResponse> rows = new ArrayList<>();
        for (MesFile file : files.listByLot(lotId)) {
            rows.add(new MesFileResponse(file.getId(), file.getFileName(), file.getSizeBytes(), file.getSha256()));
        }
        return rows;
    }

    /**
     * 把内容写到登记时的相对路径。
     *
     * @param id 文件
     * @param body 内容
     * @throws BusinessException 文件不存在，摘要或大小不符，或路径跳出根目录
     */
    public void store(Long id, byte[] body) {
        MesFile file = files.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.FILE_NOT_FOUND, "文件不存在"));
        if (body == null || body.length != file.getSizeBytes() || !digest(body).equals(file.getSha256())) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件内容与登记的大小或摘要不符");
        }
        Path target = located(file);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, body);
        } catch (IOException ex) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件内容写不进去");
        }
    }

    /**
     * 按登记路径读出内容。
     *
     * @param id 文件
     * @return 内容
     * @throws BusinessException 文件不存在，磁盘上没有内容，或路径跳出根目录
     */
    public byte[] load(Long id) {
        MesFile file = files.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.FILE_NOT_FOUND, "文件不存在"));
        Path target = located(file);
        if (!Files.isRegularFile(target)) {
            throw new BusinessException(MesErrorCodes.FILE_NOT_FOUND, "文件内容不存在");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件内容读不出来");
        }
    }

    private Path located(MesFile file) {
        Path target = root.resolve(file.getStoragePath()).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件路径不合法");
        }
        return target;
    }

    private static String digest(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception ex) {
            throw new BusinessException(MesErrorCodes.FILE_INVALID, "文件摘要算不出来");
        }
    }

    private static boolean sha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }
}
