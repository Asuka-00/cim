package com.scaffold.system.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * ArchitectureBoundaryTest测试类
 * <p>
 * 验证领域层不依赖本模块 API 或基础设施层。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-10
 */
class ArchitectureBoundaryTest {

    /**
     * 验证领域层不依赖 API 或基础设施层
     */
    @Test
    void domainDoesNotDependOnApiOrInfrastructure() throws IOException {
        List<String> violations = Files.walk(Path.of("src/main/java/com/scaffold/system/domain"))
                .filter(path -> path.toString().endsWith(".java"))
                .flatMap(this::lines)
                .filter(line -> line.matches("\\s*import com\\.scaffold\\.system\\.(api|infra)\\..*"))
                .toList();

        assertThat(violations).isEmpty();
    }

    private java.util.stream.Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取源文件: " + path, exception);
        }
    }
}
