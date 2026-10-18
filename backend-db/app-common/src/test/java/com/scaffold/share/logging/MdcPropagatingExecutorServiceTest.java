package com.scaffold.share.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * MdcPropagatingExecutorService MDC 透传单测。
 */
class MdcPropagatingExecutorServiceTest {

    @AfterEach
    void tearDown() {
        MdcUtils.clear();
    }

    @Test
    void executePropagatesTraceIdToWorker() throws Exception {
        try (ExecutorService executor = new MdcPropagatingExecutorService(Executors.newSingleThreadExecutor())) {
            MdcUtils.setTraceId("parent-trace-001");
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<String> childTrace = new AtomicReference<>();

            executor.execute(() -> {
                childTrace.set(MdcUtils.getTraceId());
                done.countDown();
            });

            assertEquals(true, done.await(3, TimeUnit.SECONDS));
            assertEquals("parent-trace-001", childTrace.get());
        }
    }

    @Test
    void submitPropagatesAndParentMdcUnchanged() throws Exception {
        try (ExecutorService executor = new MdcPropagatingExecutorService(Executors.newSingleThreadExecutor())) {
            MdcUtils.setTraceId("parent-trace-002");
            String child = executor.submit(MdcUtils::getTraceId).get(3, TimeUnit.SECONDS);
            assertEquals("parent-trace-002", child);
            assertEquals("parent-trace-002", MdcUtils.getTraceId());
        }
        MdcUtils.clear();
        assertNull(MdcUtils.getTraceId());
    }
}
