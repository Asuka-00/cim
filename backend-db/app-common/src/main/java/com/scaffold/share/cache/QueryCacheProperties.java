package com.scaffold.share.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * QueryCacheProperties 透明查询缓存配置（前缀 {@code app.cache}）。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cache")
public class QueryCacheProperties {

    /** 总开关 */
    private boolean enabled = true;

    /** Redis key 前缀 */
    private String keyPrefix = "app:qc";

    /** 默认存活时间；表版本号已保证写后失效，TTL 只兜底绕过 MyBatis 的写入 */
    private Duration defaultTtl = Duration.ofMinutes(5);

    /** 单次结果行数上限，超过则不缓存，避免大结果集占满 Redis */
    private int maxRows = 1000;

    /** 排除的表（支持 * 通配）。命中任一表的查询不缓存 */
    private List<String> excludeTables = new ArrayList<>(List.of("hist_*", "mes_*", "flyway_schema_history"));

    /** 仅缓存这些表（支持 * 通配）。为空表示不限制 */
    private List<String> includeTables = new ArrayList<>();

    /** 按表覆盖存活时间，key 为表名（小写） */
    private Map<String, Duration> tableTtl = new LinkedHashMap<>();

    /** 缓存未命中时，等待同 key 其它线程回源的最长时间（防击穿） */
    private Duration lockWait = Duration.ofSeconds(1);

    /** 连续失败多少次后熔断（期间直接查库） */
    private int breakerThreshold = 5;

    /** 熔断持续时间 */
    private Duration breakerOpen = Duration.ofSeconds(10);
}
