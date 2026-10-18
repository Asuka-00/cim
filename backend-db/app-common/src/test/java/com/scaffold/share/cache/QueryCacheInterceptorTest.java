package com.scaffold.share.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.plugin.Interceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

/**
 * 透明查询缓存：使用 H2 + 真实 MyBatis-Plus 插件链（租户 + 分页）验证命中、失效与事务语义。
 */
class QueryCacheInterceptorTest {

    @Getter
    @Setter
    @TableName("qc_item")
    public static class Item {
        @TableId(type = IdType.INPUT)
        private Long id;
        private String tenantId;
        private String name;
        private BigDecimal amount;
        private LocalDateTime createdAt;
        private StateEnum state;
    }

    @Mapper
    public interface ItemMapper extends BaseMapper<Item> {

        @NoCache
        @Select("SELECT COUNT(*) FROM qc_item")
        Long countNoCache();

        @Select("SELECT COUNT(*) FROM qc_item")
        Long countCached();
    }

    @Mapper
    public interface HistLogMapper {

        @Select("SELECT COUNT(*) FROM hist_log")
        Long count();
    }

    private DataSource dataSource;
    private InMemoryQueryCacheBackend backend;
    private SqlSessionTemplate session;
    private ItemMapper mapper;
    private HistLogMapper histMapper;
    private DataSourceTransactionManager txManager;
    private String tenant;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource = ds;
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE qc_item (id BIGINT PRIMARY KEY, tenant_id VARCHAR(32), name VARCHAR(64),"
                    + " amount DECIMAL(10,2), created_at TIMESTAMP, state INT)");
            s.execute("CREATE TABLE hist_log (id BIGINT PRIMARY KEY)");
        }
        tenant = "t1";
        backend = new InMemoryQueryCacheBackend();
        QueryCacheProperties properties = new QueryCacheProperties();
        properties.setBreakerThreshold(2);
        Interceptor cache = new QueryCacheInterceptor(properties, backend, new QueryCacheSerializer(),
                new SqlTableResolver(), null);

        MybatisPlusInterceptor mp = new MybatisPlusInterceptor();
        mp.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(tenant);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return !"qc_item".equalsIgnoreCase(tableName);
            }
        }));
        mp.addInnerInterceptor(new PaginationInnerInterceptor(com.baomidou.mybatisplus.annotation.DbType.H2));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(ItemMapper.class);
        configuration.addMapper(HistLogMapper.class);
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        factoryBean.setConfiguration(configuration);
        // 与 Spring 的 @Order 排序结果一致：缓存插件在内层（先注册），MP 在外层
        factoryBean.setPlugins(cache, mp);
        session = new SqlSessionTemplate(factoryBean.getObject());
        mapper = session.getMapper(ItemMapper.class);
        histMapper = session.getMapper(HistLogMapper.class);
        txManager = new DataSourceTransactionManager(ds);
    }

    private void rawSql(String sql) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private Item item(long id, String name) {
        Item item = new Item();
        item.setId(id);
        item.setTenantId("t1");
        item.setName(name);
        item.setAmount(new BigDecimal("12.50"));
        item.setCreatedAt(LocalDateTime.of(2026, 10, 15, 8, 30));
        item.setState(StateEnum.ENABLED);
        return item;
    }

    @Test
    void secondReadIsServedFromCacheAndRoundTripsTypes() throws Exception {
        mapper.insert(item(1, "a"));
        Item first = mapper.selectById(1L);
        // 绕过 MyBatis 改库：若第二次读走了缓存，仍应看到旧值
        rawSql("UPDATE qc_item SET name = 'raw' WHERE id = 1");
        Item second = mapper.selectById(1L);

        assertThat(second.getName()).isEqualTo("a").isEqualTo(first.getName());
        assertThat(second.getAmount()).isEqualByComparingTo("12.50");
        assertThat(second.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 15, 8, 30));
        assertThat(second.getState()).isEqualTo(StateEnum.ENABLED);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    void writeThroughMapperInvalidatesCache() throws Exception {
        mapper.insert(item(1, "a"));
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");

        Item changed = item(1, "b");
        mapper.updateById(changed);

        assertThat(mapper.selectById(1L).getName()).isEqualTo("b");
    }

    @Test
    void scalarLongSurvivesRoundTrip() {
        mapper.insert(item(1, "a"));
        Long first = mapper.countCached();
        Long second = mapper.countCached();
        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(1L);
    }

    @Test
    void noCacheAnnotationBypasses() throws Exception {
        mapper.insert(item(1, "a"));
        assertThat(mapper.countNoCache()).isEqualTo(1L);
        rawSql("INSERT INTO qc_item VALUES (2, 't1', 'x', 1, NULL, 1)");
        assertThat(mapper.countNoCache()).isEqualTo(2L);
    }

    @Test
    void excludedTablesAreNotCached() throws Exception {
        assertThat(histMapper.count()).isZero();
        rawSql("INSERT INTO hist_log VALUES (1)");
        assertThat(histMapper.count()).isEqualTo(1L);
    }

    @Test
    void tenantsDoNotShareEntries() {
        mapper.insert(item(1, "mine"));
        assertThat(mapper.selectById(1L)).isNotNull();

        tenant = "t2";
        assertThat(mapper.selectById(1L)).isNull();
    }

    @Test
    void differentPagesUseDifferentKeys() {
        for (long i = 1; i <= 5; i++) {
            mapper.insert(item(i, "n" + i));
        }
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>().orderByAsc(Item::getId);
        IPage<Item> page1 = mapper.selectPage(new Page<>(1, 2), wrapper);
        IPage<Item> page2 = mapper.selectPage(new Page<>(2, 2), wrapper);
        IPage<Item> page1Again = mapper.selectPage(new Page<>(1, 2), wrapper);

        assertThat(page1.getRecords()).extracting(Item::getId).containsExactly(1L, 2L);
        assertThat(page2.getRecords()).extracting(Item::getId).containsExactly(3L, 4L);
        assertThat(page1Again.getRecords()).extracting(Item::getId).containsExactly(1L, 2L);
        assertThat(page1.getTotal()).isEqualTo(5L);
        assertThat(page2.getTotal()).isEqualTo(5L);
    }

    @Test
    void readsInsideTransactionSeeOwnWritesAndCommitInvalidates() {
        mapper.insert(item(1, "a"));
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            mapper.updateById(item(1, "tx"));
            assertThat(mapper.selectById(1L).getName()).isEqualTo("tx");
        });

        assertThat(mapper.selectById(1L).getName()).isEqualTo("tx");
    }

    @Test
    void rollbackDoesNotLeaveDirtyStateOrStaleEntries() {
        mapper.insert(item(1, "a"));
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            mapper.updateById(item(1, "gone"));
            status.setRollbackOnly();
        });

        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");
    }

    @Test
    void backendFailureFallsBackToDatabase() {
        mapper.insert(item(1, "a"));
        backend.failing = true;

        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");
        assertThat(mapper.selectList(null)).hasSize(1);
        // 熔断后同样可用
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");
    }

    @Test
    void invalidatorBumpsVersions() throws Exception {
        mapper.insert(item(1, "a"));
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");
        rawSql("UPDATE qc_item SET name = 'raw' WHERE id = 1");
        assertThat(mapper.selectById(1L).getName()).isEqualTo("a");

        new QueryCacheInvalidator(backend).invalidateTables(List.of("QC_ITEM"));

        assertThat(mapper.selectById(1L).getName()).isEqualTo("raw");
    }

}
