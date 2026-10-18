package com.scaffold.share.logging;

import java.util.Map;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 将提交线程的 MDC 拷贝到执行线程，执行后恢复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-29
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> context = MdcUtils.captureContext();
        return () -> MdcUtils.runWithContext(context, runnable);
    }
}
