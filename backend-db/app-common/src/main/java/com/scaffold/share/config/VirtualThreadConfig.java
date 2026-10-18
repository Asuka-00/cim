package com.scaffold.share.config;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import com.scaffold.share.logging.MdcPropagatingExecutorService;
import com.scaffold.share.logging.MdcTaskDecorator;

/**
 * VirtualThreadConfig 虚拟线程配置。
 * <p>
 * 提供 JDK 21 虚拟线程执行器，并自动透传提交线程 MDC（traceId 等）。
 */
@Configuration
public class VirtualThreadConfig {

    /**
     * 虚拟线程 ExecutorService（带 MDC 透传）。
     */
    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutorService() {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("app-vt-", 0)
                .factory();
        ExecutorService raw = Executors.newThreadPerTaskExecutor(threadFactory);
        return new MdcPropagatingExecutorService(raw);
    }

    /**
     * 虚拟线程 AsyncTaskExecutor（带 MDC TaskDecorator）。
     */
    @Bean
    public AsyncTaskExecutor virtualThreadTaskExecutor(ExecutorService virtualThreadExecutorService) {
        TaskExecutorAdapter adapter = new TaskExecutorAdapter(Objects.requireNonNull(virtualThreadExecutorService));
        // Executor 已包装 MDC；再套 TaskDecorator 对直接 execute 路径双重包装无害（空父上下文）
        adapter.setTaskDecorator(new MdcTaskDecorator());
        return adapter;
    }
}
