package com.scaffold.system.domain.model.system;

import lombok.Getter;
import lombok.Setter;

/**
 * version.json 映射。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Getter
@Setter
public class ReleaseVersionMeta {

    private String version;

    private String buildTime;

    private String gitCommit;

    private String jarSha256;
}
