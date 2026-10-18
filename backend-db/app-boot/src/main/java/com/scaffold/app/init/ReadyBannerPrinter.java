package com.scaffold.app.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * ReadyBannerPrinter 应用完全启动后输出霓虹色启动 Banner 与就绪信息。
 * <p>
 * 监听 {@link ApplicationReadyEvent}（就绪之后），而非 Spring Boot 启动早期 Banner。
 * Banner 仅做 ASCII 着色；启动成功描述与 Swagger 地址在 Banner 之后单独打印，互不影响。
 *
 * @author Kairo
 * @version 1.2.0
 * @since 2026-07-29
 */
@Component
public class ReadyBannerPrinter {

    private static final Logger log = LoggerFactory.getLogger(ReadyBannerPrinter.class);

    private static final String BANNER_RESOURCE = "banner-ready.txt";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[38;5;46m";
    private static final String ANSI_CYAN = "\u001B[38;5;51m";
    private static final String ANSI_DIM = "\u001B[38;5;245m";
    /** 霓虹青 / 亮粉 / 品红 / 紫 / 电蓝 / 亮青 */
    private static final int[] NEON_PALETTE = { 51, 207, 201, 165, 135, 45 };

    private final Environment environment;

    public ReadyBannerPrinter(Environment environment) {
        this.environment = environment;
    }

    /**
     * 应用就绪后打印霓虹 ASCII Banner，再输出启动成功与 Swagger 访问信息。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onApplicationReady() {
        printBanner();
        printReadyInfo();
    }

    private void printBanner() {
        try {
            ClassPathResource resource = new ClassPathResource(BANNER_RESOURCE);
            if (!resource.exists()) {
                log.warn("Startup banner resource missing: {}", BANNER_RESOURCE);
                return;
            }
            try (InputStream in = resource.getInputStream()) {
                String banner = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                System.out.println();
                System.out.print(colorizeNeon(banner));
                if (!banner.endsWith("\n")) {
                    System.out.println();
                }
                System.out.println();
            }
        } catch (IOException ex) {
            log.warn("Failed to print startup banner: {}", ex.getMessage());
        }
    }

    /**
     * 在 Banner 之后输出启动成功描述与 Swagger 地址（不改动 ASCII Banner 本身）。
     */
    private void printReadyInfo() {
        String appName = environment.getProperty("spring.application.name", "app-backend");
        String port = resolveServerPort();
        String swaggerPath = normalizePath(
                environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html"));
        String apiDocsPath = normalizePath(
                environment.getProperty("springdoc.api-docs.path", "/v3/api-docs"));
        String profile = resolveActiveProfiles();
        String swaggerUrl = "http://localhost:" + port + swaggerPath;
        String apiDocsUrl = "http://localhost:" + port + apiDocsPath;

        System.out.println(ANSI_GREEN + "  :: Application Started Successfully ::" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "  Application : " + ANSI_RESET + appName);
        System.out.println(ANSI_CYAN + "  Profile     : " + ANSI_RESET + profile);
        System.out.println(ANSI_CYAN + "  Port        : " + ANSI_RESET + port);
        System.out.println(ANSI_CYAN + "  Swagger UI  : " + ANSI_RESET + swaggerUrl);
        System.out.println(ANSI_DIM + "  API Docs    : " + ANSI_RESET + apiDocsUrl);
        System.out.println();
    }

    private String resolveServerPort() {
        // local.server.port 为嵌入式容器实际绑定端口（支持 random port）
        String localPort = environment.getProperty("local.server.port");
        if (StringUtils.hasText(localPort)) {
            return localPort;
        }
        return environment.getProperty("server.port", "8080");
    }

    private String resolveActiveProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(",", profiles);
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 按行套霓虹 256 色；逐字微调色阶，形成轻微扫描渐变。
     */
    static String colorizeNeon(String banner) {
        if (banner == null || banner.isEmpty()) {
            return banner;
        }
        String[] lines = banner.split("\\R", -1);
        StringBuilder out = new StringBuilder(banner.length() * 4);
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            if (line.isEmpty()) {
                out.append('\n');
                continue;
            }
            int base = NEON_PALETTE[lineIdx % NEON_PALETTE.length];
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == ' ') {
                    out.append(ch);
                    continue;
                }
                // 同行内轻微偏移，避免整行死板同色
                int color = NEON_PALETTE[(lineIdx + i / 12) % NEON_PALETTE.length];
                if (i == 0) {
                    color = base;
                }
                out.append("\u001B[38;5;").append(color).append('m').append(ch);
            }
            out.append(ANSI_RESET);
            if (lineIdx < lines.length - 1) {
                out.append('\n');
            }
        }
        return out.toString();
    }
}
