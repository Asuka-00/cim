package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.ToolApplicationService;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;

import lombok.RequiredArgsConstructor;

/**
 * ToolLifePortImpl 工具寿命
 * <p>
 * 转给工具服务判断。没有上限时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class ToolLifePortImpl implements ToolLifePort {

    private final ToolApplicationService tools;

    @Override
    public boolean withinLife(Long toolId) {
        return tools.withinLife(toolId);
    }

    @Override
    public void use(Long toolId) {
        tools.use(toolId);
    }

    @Override
    public void reverse(Long toolId) {
        tools.reverse(toolId);
    }
}
