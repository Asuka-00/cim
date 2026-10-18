package com.scaffold.mes.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * domain 不得依赖本模块的 api 或 infra。业务模块之间不得互相引用。
 */
class MesModuleBoundaryTest {

    @Test
    void domainDoesNotImportApiOrInfra() throws IOException {
        Path domain = Path.of("src/main/java/com/scaffold/mes/core/domain");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(domain)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                if (text.contains("com.scaffold.mes.core.api") || text.contains("com.scaffold.mes.core.infra")) {
                    offenders.add(file.toString());
                }
            }
        }
        assertThat(offenders).isEmpty();
    }

    @Test
    void businessModulesDoNotImportEachOther() throws IOException {
        List<String> siblings = List.of("app-mes-base", "app-mes-warehouse", "app-mes-exec");
        List<String> offenders = new ArrayList<>();
        for (String module : siblings) {
            offenders.addAll(importsOf(module, siblings));
        }
        offenders.addAll(importsOf("app-mes-core", siblings));
        assertThat(offenders).isEmpty();
    }

    /**
     * 收集模块对其他 MES 包的引用。base、warehouse、exec 可以依赖 core，不能互相依赖。core 不能反向依赖它们。
     */
    private static List<String> importsOf(String module, List<String> forbidden) throws IOException {
        List<String> offenders = new ArrayList<>();
        Path root = Path.of("..", module, "src/main/java");
        if (!Files.isDirectory(root)) {
            return offenders;
        }
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                for (String other : forbidden) {
                    if (other.equals(module)) {
                        continue;
                    }
                    String pkg = other.replace("app-mes-", "com.scaffold.mes.");
                    if (text.contains(pkg)) {
                        offenders.add(file + " -> " + pkg);
                    }
                }
            }
        }
        return offenders;
    }
}
