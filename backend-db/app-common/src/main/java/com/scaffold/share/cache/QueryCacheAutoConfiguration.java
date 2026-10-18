package com.scaffold.share.cache;

import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * QueryCacheAutoConfiguration 透明查询缓存装配。
 * <p>
 * 通过 {@code app.cache.enabled=false} 关闭。拦截器以最高优先级注册，
 * 位于 MybatisPlusInterceptor 内层，缓存 key 基于已叠加租户条件与分页的最终 SQL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(QueryCacheProperties.class)
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(StringRedisTemplate.class)
public class QueryCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QueryCacheBackend queryCacheBackend(StringRedisTemplate redis, QueryCacheProperties properties) {
        return new RedisQueryCacheBackend(redis, properties.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryCacheInvalidator queryCacheInvalidator(QueryCacheBackend backend) {
        return new QueryCacheInvalidator(backend);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Interceptor queryCacheInterceptor(QueryCacheProperties properties, QueryCacheBackend backend,
            ObjectProvider<MeterRegistry> meterRegistry) {
        return new QueryCacheInterceptor(properties, backend, new QueryCacheSerializer(), new SqlTableResolver(),
                meterRegistry.getIfAvailable());
    }
}
