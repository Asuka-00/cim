package com.scaffold.system.domain.log;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LogTailVO 日志尾部内容。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Getter
@AllArgsConstructor
public class LogTailVO {

    private final String fileName;

    private final String type;

    private final int lines;

    private final List<String> content;
}
