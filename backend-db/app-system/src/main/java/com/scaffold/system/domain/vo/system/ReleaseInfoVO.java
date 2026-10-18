package com.scaffold.system.domain.vo.system;

import lombok.Getter;
import lombok.Setter;

/**
 * 已安装发布版本信息。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Getter
@Setter
public class ReleaseInfoVO {

    private String version;

    private String buildTime;

    private String gitCommit;

    private String jarSha256;

    /** jar 字节大小 */
    private long jarSizeBytes;

    private boolean current;

    private boolean previous;
}
