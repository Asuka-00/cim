package com.scaffold.system.domain.log;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LogFileInfoVO 日志文件信息。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Getter
@AllArgsConstructor
public class LogFileInfoVO {

    private final String fileName;

    private final String type;

    private final long size;

    private final LocalDateTime lastModifiedAt;

    private final boolean compressed;
}
