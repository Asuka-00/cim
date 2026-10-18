package com.scaffold.system.domain.query.system;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * SystemConfigQuery类
 * <p>
 * 负责处理SystemConfigQuery相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class SystemConfigQuery {

    private String configCode;

    private String configValue;

    private String valueType;

    private String groupCode;

    private StateEnum state;
}
