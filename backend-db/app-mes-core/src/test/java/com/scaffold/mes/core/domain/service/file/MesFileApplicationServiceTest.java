package com.scaffold.mes.core.domain.service.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.file.MesFileRegisterRequest;
import com.scaffold.mes.core.domain.dto.file.MesFileResponse;
import com.scaffold.mes.core.domain.entity.file.MesFile;
import com.scaffold.mes.core.domain.repository.file.MesFileRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 合法摘要会挂到 Lot 上。摘要不是 64 位十六进制时拒绝。
 */
class MesFileApplicationServiceTest {

    private MesFileRepository files;
    private MesFileApplicationService service;
    private Path root;

    @BeforeEach
    void setUp(@TempDir Path root) {
        this.root = root;
        files = mock(MesFileRepository.class);
        service = new MesFileApplicationService(files, root.toString());
    }

    @Test
    void registersMetadataOnLot() {
        service.register(request("a".repeat(64)));

        verify(files).save(org.mockito.ArgumentMatchers.<MesFile>argThat(row ->
                "LOT".equals(row.getBizType()) && row.getBizId().equals(8L)
                        && row.getStoragePath().equals("lot/8/" + "a".repeat(64))
                        && row.getFileName().equals("map.txt")));
    }

    @Test
    void shortDigestRejected() {
        assertThatThrownBy(() -> service.register(request("abc")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.FILE_INVALID);
    }

    @Test
    void listsFilesOnTheLot() {
        MesFile file = new MesFile();
        file.setId(3L);
        file.setFileName("map.txt");
        file.setSizeBytes(12L);
        file.setSha256("a".repeat(64));
        when(files.listByLot(8L)).thenReturn(List.of(file));

        assertThat(service.listByLot(8L)).containsExactly(new MesFileResponse(3L, "map.txt", 12L, "a".repeat(64)));
    }

    @Test
    void storesBytesAtTheRegisteredPath() throws Exception {
        MesFile file = new MesFile();
        file.setSizeBytes(5L);
        file.setSha256("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        file.setStoragePath("lot/8/" + file.getSha256());
        when(files.findById(3L)).thenReturn(Optional.of(file));

        service.store(3L, "hello".getBytes());

        assertThat(Files.readString(root.resolve(file.getStoragePath()))).isEqualTo("hello");
        assertThat(service.load(3L)).isEqualTo("hello".getBytes());
    }

    @Test
    void missingBytesRejected() {
        MesFile file = new MesFile();
        file.setStoragePath("lot/8/missing");
        when(files.findById(3L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.load(3L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.FILE_NOT_FOUND);
    }

    @Test
    void mismatchedDigestRejected() {
        MesFile file = new MesFile();
        file.setSizeBytes(5L);
        file.setSha256("a".repeat(64));
        file.setStoragePath("lot/8/x");
        when(files.findById(3L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.store(3L, "hello".getBytes()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.FILE_INVALID);
    }

    private static MesFileRegisterRequest request(String sha) {
        MesFileRegisterRequest request = new MesFileRegisterRequest();
        request.setFileName("map.txt");
        request.setSizeBytes(12L);
        request.setSha256(sha);
        request.setLotId(8L);
        return request;
    }
}
