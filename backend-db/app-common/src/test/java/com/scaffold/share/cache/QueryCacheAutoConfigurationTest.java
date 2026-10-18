package com.scaffold.share.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.plugin.Interceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

/**
 * 装配与插件顺序：缓存插件必须排在 MybatisPlusInterceptor 之前（内层），否则 key 缺少租户与分页。
 */
class QueryCacheAutoConfigurationTest {

    @Configuration
    static class Infra {
        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Infra.class)
            .withConfiguration(AutoConfigurations.of(QueryCacheAutoConfiguration.class));

    @Test
    void cachePluginIsOrderedBeforeMybatisPlus() {
        runner.run(context -> {
            List<Class<?>> order = context.getBeanProvider(Interceptor.class).orderedStream()
                    .map(Object::getClass).collect(Collectors.toList());
            assertThat(order).containsExactly(QueryCacheInterceptor.class, MybatisPlusInterceptor.class);
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("app.cache.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(QueryCacheInterceptor.class);
            assertThat(context).doesNotHaveBean(QueryCacheInvalidator.class);
        });
    }

    @Test
    void backsOffWithoutRedis() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(QueryCacheAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(QueryCacheInterceptor.class));
    }
}
