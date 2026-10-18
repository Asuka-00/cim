package com.scaffold.system.domain.vo.system;

import lombok.Getter;
import lombok.Setter;

/**
 * 激活 / 回滚结果。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Getter
@Setter
public class ReleaseActivateResultVO {

    private String version;

    private String previousVersion;

    private long restartDelayMs;

    private String message;
}
