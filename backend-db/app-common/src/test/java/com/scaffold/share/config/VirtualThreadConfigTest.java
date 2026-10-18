package com.scaffold.share.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

class VirtualThreadConfigTest {

    @Test
    void createsNamedVirtualThreadExecutor() throws Exception {
        VirtualThreadConfig config = new VirtualThreadConfig();

        try (ExecutorService executorService = config.virtualThreadExecutorService()) {
            AtomicReference<Thread> threadRef = new AtomicReference<>();
            executorService.submit(() -> threadRef.set(Thread.currentThread())).get(1, TimeUnit.SECONDS);

            Thread thread = threadRef.get();
            assertThat(thread.isVirtual()).isTrue();
            assertThat(thread.getName()).startsWith("app-vt-");
        }
    }

    @Test
    void asyncTaskExecutorReusesVirtualThreadExecutor() throws Exception {
        VirtualThreadConfig config = new VirtualThreadConfig();

        try (ExecutorService executorService = config.virtualThreadExecutorService()) {
            AsyncTaskExecutor taskExecutor = config.virtualThreadTaskExecutor(executorService);
            AtomicReference<Thread> threadRef = new AtomicReference<>();

            taskExecutor.submit(() -> threadRef.set(Thread.currentThread())).get(1, TimeUnit.SECONDS);

            assertThat(threadRef.get().isVirtual()).isTrue();
            assertThat(threadRef.get().getName()).startsWith("app-vt-");
        }
    }
}
