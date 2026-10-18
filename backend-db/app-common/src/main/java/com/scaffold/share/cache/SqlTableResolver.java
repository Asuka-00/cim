package com.scaffold.share.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.lang.Nullable;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * SqlTableResolver 从 SQL 文本解析涉及的表名（小写、去引号与 schema）。
 * <p>
 * 解析结果按 SQL 文本做有界 LRU 缓存；无法解析时返回 null，调用方应放弃缓存（读）或全局失效（写）。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
public class SqlTableResolver {

    private static final int MAX_ENTRIES = 4096;
    private static final Set<String> UNPARSEABLE = Collections.emptySet();

    private final Map<String, Set<String>> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    /**
     * 解析表名集合（有序），失败返回 null
     */
    @Nullable
    public Set<String> tables(String sql) {
        Set<String> cached = cache.get(sql);
        if (cached == null) {
            cached = parse(sql);
            cache.put(sql, cached);
        }
        return cached == UNPARSEABLE || cached.isEmpty() ? null : cached;
    }

    private Set<String> parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Set<String> names = new TreeSet<>();
            for (String raw : new TablesNamesFinder<Void>().getTables(statement)) {
                names.add(normalize(raw));
            }
            return Collections.unmodifiableSet(names);
        } catch (Exception | StackOverflowError ex) {
            return UNPARSEABLE;
        }
    }

    static String normalize(String raw) {
        String name = raw;
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        return name.replace("`", "").replace("\"", "").replace("[", "").replace("]", "")
                .trim().toLowerCase(Locale.ROOT);
    }
}
