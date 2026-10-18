package com.scaffold.system.domain.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.scaffold.share.exception.BusinessException;

class LogFileServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void listFilesReturnsOnlyMatchingType() throws Exception {
        Files.writeString(tempDir.resolve("audit.log"), "audit", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("audit.2026-06-08.0.log.gz"), "archive", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("error.log"), "error", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("performance.log"), "slow", StandardCharsets.UTF_8);

        LogFileService service = new LogFileService(tempDir.toString());

        List<LogFileInfoVO> files = service.listFiles("audit");

        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(file -> "audit".equals(file.getType())));
        assertTrue(files.stream().anyMatch(file -> file.isCompressed()));
    }

    @Test
    void slowTypeMapsToPerformanceFiles() throws Exception {
        Files.writeString(tempDir.resolve("performance.log"), "slow", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("performance.2026-06-08.0.log.gz"), "archive", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("audit.log"), "audit", StandardCharsets.UTF_8);

        LogFileService service = new LogFileService(tempDir.toString());

        List<LogFileInfoVO> files = service.listFiles("slow");

        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(file -> "slow".equals(file.getType())));
        assertTrue(files.stream().allMatch(file -> file.getFileName().startsWith("performance")));
    }

    @Test
    void tailReturnsLastRequestedLines() throws Exception {
        Files.writeString(tempDir.resolve("error.log"), "one\ntwo\nthree\nfour\n", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString());

        LogTailVO tail = service.tail("error", "error.log", 2);

        assertEquals("error.log", tail.getFileName());
        assertEquals("error", tail.getType());
        assertEquals(2, tail.getLines());
        assertEquals(List.of("three", "four"), tail.getContent());
    }

    @Test
    void tailAllReturnsCompleteFile() throws Exception {
        Files.writeString(tempDir.resolve("audit.log"), "one\ntwo\nthree\n", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString());

        LogTailVO tail = service.tail("audit", "audit.log", "ALL");

        assertEquals("audit.log", tail.getFileName());
        assertEquals("audit", tail.getType());
        assertEquals(3, tail.getLines());
        assertEquals(List.of("one", "two", "three"), tail.getContent());
    }

    @Test
    void tailAllRejectsFileOverConfiguredLimit() throws Exception {
        Files.writeString(tempDir.resolve("audit.log"), "one\ntwo\n", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString(), 3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.tail("audit", "audit.log", "ALL"));

        assertTrue(ex.getMessage().contains("日志文件过大"));
    }

    @Test
    void tailCapsRequestedLines() throws Exception {
        Files.writeString(tempDir.resolve("audit.log"), "one\n", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString());

        LogTailVO tail = service.tail("audit", "audit.log", 6000);

        assertEquals(5000, tail.getLines());
        assertEquals(List.of("one"), tail.getContent());
    }

    @Test
    void tailRejectsCompressedFile() throws Exception {
        Files.writeString(tempDir.resolve("performance.2026-06-08.0.log.gz"), "archive", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.tail("slow", "performance.2026-06-08.0.log.gz", 10));

        assertTrue(ex.getMessage().contains("压缩日志不支持在线查看"));
    }

    @Test
    void downloadRejectsPathTraversalAndWrongType() throws Exception {
        Files.writeString(tempDir.resolve("audit.log"), "audit", StandardCharsets.UTF_8);
        LogFileService service = new LogFileService(tempDir.toString());

        assertThrows(BusinessException.class, () -> service.download("audit", "../audit.log"));
        assertThrows(BusinessException.class, () -> service.download("error", "audit.log"));
    }

    @Test
    void missingFileThrowsBusinessException() {
        LogFileService service = new LogFileService(tempDir.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.download("audit", "audit.log"));

        assertTrue(ex.getMessage().contains("日志文件不存在"));
    }
}
