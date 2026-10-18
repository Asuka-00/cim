package com.scaffold.system.domain.vo.system;

import lombok.Getter;
import lombok.Setter;

/**
 * 当前激活版本与进程内运行版本对照。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Getter
@Setter
public class ReleaseCurrentVO {

    /** current 软链指向的版本 */
    private String currentVersion;

    private String buildTime;

    private String gitCommit;

    private String jarSha256;

    /** previous 软链指向的版本，可能为空 */
    private String previousVersion;

    /**
     * 当前 JVM 进程解析到的实现版本（可能与 soft-link 不一致，例如刚激活未重启）。
     */
    private String runningVersion;
}
